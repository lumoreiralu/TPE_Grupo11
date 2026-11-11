package ticketya.compras_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ticketya.compras_service.client.*;
import ticketya.compras_service.dto.*;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.model.*;
import ticketya.compras_service.repository.BloqueoAsientoRepository;
import ticketya.compras_service.repository.CupoSectorRepository;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private final OrdenCompraRepository ordenRepo;
    private final BloqueoAsientoRepository bloqueoRepo;
    private final CupoSectorRepository cupoSectorRepo;
    private final EventosClient eventosClient;
    private final PasarelaClient pasarelaClient;
    private final EntradasClient entradasClient;
    private final CompraTransaccionService transaccionService;

    @Value("${compras.reserva.minutos}")
    private int minutosReserva;

    @Value("${compras.limite-entradas-por-cliente}")
    private int limiteEntradas;

    public ReservaService(OrdenCompraRepository ordenRepo,
                          BloqueoAsientoRepository bloqueoRepo,
                          CupoSectorRepository cupoSectorRepo,
                          EventosClient eventosClient,
                          PasarelaClient pasarelaClient,
                          EntradasClient entradasClient,
                          CompraTransaccionService transaccionService) {
        this.ordenRepo = ordenRepo;
        this.bloqueoRepo = bloqueoRepo;
        this.cupoSectorRepo = cupoSectorRepo;
        this.eventosClient = eventosClient;
        this.pasarelaClient = pasarelaClient;
        this.entradasClient = entradasClient;
        this.transaccionService = transaccionService;
    }

    @Transactional
    public ReservaResponse reservar(ReservaRequest req, String idempotencyKey) {
        // 1. Reintento: si ya existe una orden con esta clave, devolvemos la misma
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existente = ordenRepo.findByIdempotencyKey(idempotencyKey);
            if (existente.isPresent()) {
                return aRespuesta(existente.get());
            }
        }

        validar(req);

        // 2. Las reservas vencidas liberan sus asientos y cupos antes de intentar tomarlos
        liberarReservasVencidas();

        // 3. El evento tiene que existir y estar publicado
        EventoDTO evento = eventosClient.obtenerEvento(req.eventoId());
        if (!"PUBLICADO".equals(evento.estado())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El evento no esta publicado");
        }

        // 4. Limite de entradas por cliente para este evento
        long yaTiene = ordenRepo.contarEntradasDelCliente(
                req.clienteId(), req.eventoId(),
                List.of(EstadoOrden.RESERVADA_TEMPORAL, EstadoOrden.PROCESANDO_PAGO,
                        EstadoOrden.PAGO_CONFIRMADO, EstadoOrden.COMPLETADA));
        if (yaTiene + req.items().size() > limiteEntradas) {
            throw new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Supera el maximo de " + limiteEntradas + " entradas por cliente para este evento");
        }

        // 5. Validación y decremento atómico de cupos para sectores generales (Campo / No numerado)
        Map<String, Long> generalesPorSector = req.items().stream()
                .filter(item -> esSectorGeneral(evento, item))
                .collect(Collectors.groupingBy(ItemReserva::sectorId, Collectors.counting()));

        for (Map.Entry<String, Long> entry : generalesPorSector.entrySet()) {
            String sectorId = entry.getKey();
            int cantidadSolicitada = entry.getValue().intValue();
            int capacidadMaxima = obtenerCapacidadMaxima(evento, sectorId);

            transaccionService.asegurarCupoSector(req.eventoId(), sectorId, capacidadMaxima);

            int filasActualizadas = cupoSectorRepo.decrementarCupo(req.eventoId(), sectorId, cantidadSolicitada);
            if (filasActualizadas == 0) {
                throw new NegocioException(HttpStatus.CONFLICT,
                        "No hay cupos disponibles suficientes en el sector general " + sectorId);
            }
        }

        // 6. Armamos la orden con su detalle y el monto total
        LocalDateTime ahora = LocalDateTime.now();
        OrdenCompra orden = new OrdenCompra();
        orden.setClienteId(req.clienteId());
        orden.setEventoId(req.eventoId());
        orden.setFechaCreacion(ahora);
        orden.setFechaExpiracionReserva(ahora.plusMinutes(minutosReserva));
        orden.setEstado(EstadoOrden.RESERVADA_TEMPORAL);
        orden.setIdempotencyKey(
                idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey);

        BigDecimal total = BigDecimal.ZERO;
        for (ItemReserva item : req.items()) {
            BigDecimal precio = precioDelSector(evento, item.sectorId());
            DetalleReserva detalle = new DetalleReserva();
            detalle.setSectorId(item.sectorId());
            detalle.setFila(item.fila());
            detalle.setAsiento(item.asiento());
            detalle.setPrecioUnitario(precio);
            orden.getDetalles().add(detalle);
            total = total.add(precio);
        }
        orden.setMontoTotal(total);
        orden = ordenRepo.saveAndFlush(orden);

        // 7. Bloqueos de asientos o cupos:
        //    - Numerados: clave compuesta (evento|sector|fila|asiento). La restricción ÚNICA previene doble venta.
        //    - Generales: clave única por item de orden para registrar la reserva temporal.
        int indiceGeneral = 0;
        for (ItemReserva item : req.items()) {
            boolean esGeneral = esSectorGeneral(evento, item);
            BloqueoAsiento bloqueo = new BloqueoAsiento();
            bloqueo.setEventoId(req.eventoId());
            bloqueo.setSectorId(item.sectorId());
            bloqueo.setOrdenId(orden.getId());
            bloqueo.setEstado(EstadoBloqueo.BLOQUEADO);
            bloqueo.setExpiraEn(orden.getFechaExpiracionReserva());

            if (esGeneral) {
                bloqueo.setClaveAsiento(claveGeneral(req.eventoId(), item.sectorId(), orden.getId(), indiceGeneral++));
            } else {
                bloqueo.setClaveAsiento(claveAsiento(req.eventoId(), item));
            }

            try {
                bloqueoRepo.saveAndFlush(bloqueo);
            } catch (DataIntegrityViolationException e) {
                throw new NegocioException(HttpStatus.CONFLICT,
                        "El asiento " + item.sectorId() + " fila " + item.fila()
                                + " nro " + item.asiento() + " ya no esta disponible");
            }
        }

        return aRespuesta(orden);
    }

    // Confirmación de pago con límites transaccionales independientes (sin llamadas HTTP dentro de transacciones de BD)
    public ConfirmacionPagoResponse confirmarPago(UUID ordenId, ConfirmarPagoRequest req) {
        // 1. Iniciar procesamiento (atómico, evita concurrencia y valida expiración)
        OrdenCompra orden = transaccionService.iniciarProcesamientoPago(ordenId);

        if (orden.getEstado() == EstadoOrden.EXPIRADA) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "La reserva temporal ha expirado y los asientos han sido liberados");
        }

        if (orden.getEstado() == EstadoOrden.COMPLETADA) {
            List<EmisionResponse.EntradaDTO> entradas = entradasClient.obtenerEntradasPorOrden(orden.getId().toString());
            return new ConfirmacionPagoResponse(
                    orden.getId(),
                    orden.getEstado(),
                    orden.getTransaccionPagoId(),
                    orden.getMontoTotal(),
                    orden.getMetodoPago(),
                    entradas,
                    "La orden ya fue completada previamente"
            );
        }

        String metodoPago = (req != null && req.metodoPago() != null && !req.metodoPago().isBlank())
                ? req.metodoPago()
                : "TARJETA";

        // 2. Si no tenía pago confirmado previamente, procesar con la pasarela mock
        if (orden.getEstado() != EstadoOrden.PAGO_CONFIRMADO) {
            PagoRequest pagoRequest = new PagoRequest(orden.getMontoTotal(), orden.getId().toString(), metodoPago);
            PagoResponse pagoResponse;
            try {
                pagoResponse = pasarelaClient.procesarPago(pagoRequest);
            } catch (Exception e) {
                transaccionService.revertirAEstadoReservado(ordenId);
                throw e;
            }

            if (pagoResponse == null || !"APROBADO".equalsIgnoreCase(pagoResponse.estado())) {
                String transId = pagoResponse != null ? pagoResponse.transaccionId() : null;
                String motivo = (pagoResponse != null && pagoResponse.mensaje() != null)
                        ? pagoResponse.mensaje()
                        : "Pago rechazado por la pasarela";

                // R5.3: El rechazo libera inmediatamente los asientos/cupos y cancela la orden de forma confirmada en BD
                transaccionService.registrarPagoRechazado(ordenId, transId, metodoPago);
                throw new NegocioException(HttpStatus.PAYMENT_REQUIRED, "El pago fue rechazado: " + motivo);
            }

            // Pago APROBADO: persistimos inmediatamente el pago y la venta definitiva de los asientos
            transaccionService.registrarPagoAprobado(ordenId, pagoResponse.transaccionId(), metodoPago);
            orden = transaccionService.obtenerOrden(ordenId);
        }

        // 3. Emisión de entradas en entradas-service (fuera de la transacción de BD)
        // Si falla aquí, el cobro ya está registrado como PAGO_CONFIRMADO; un reintento no vuelve a cobrar
        List<EmisionRequest.AsientoEntrada> asientos = orden.getDetalles().stream()
                .map(d -> new EmisionRequest.AsientoEntrada(d.getSectorId(), d.getFila(), d.getAsiento()))
                .toList();
        EmisionRequest emisionReq = new EmisionRequest(
                orden.getId().toString(),
                orden.getClienteId(),
                orden.getEventoId(),
                asientos
        );

        EmisionResponse emisionResp = entradasClient.emitirEntradas(emisionReq);
        List<EmisionResponse.EntradaDTO> entradas = (emisionResp != null && emisionResp.entradas() != null)
                ? emisionResp.entradas()
                : List.of();

        // 4. Marcar orden como COMPLETADA
        transaccionService.registrarOrdenCompletada(ordenId);
        orden = transaccionService.obtenerOrden(ordenId);

        return new ConfirmacionPagoResponse(
                orden.getId(),
                orden.getEstado(),
                orden.getTransaccionPagoId(),
                orden.getMontoTotal(),
                orden.getMetodoPago(),
                entradas,
                "Compra completada exitosamente"
        );
    }

    public ConfirmacionPagoResponse consultarOrden(UUID ordenId) {
        OrdenCompra orden = transaccionService.obtenerOrden(ordenId);
        List<EmisionResponse.EntradaDTO> entradas = List.of();
        if (orden.getEstado() == EstadoOrden.COMPLETADA || orden.getEstado() == EstadoOrden.PAGO_CONFIRMADO) {
            entradas = entradasClient.obtenerEntradasPorOrden(orden.getId().toString());
        }
        return new ConfirmacionPagoResponse(
                orden.getId(),
                orden.getEstado(),
                orden.getTransaccionPagoId(),
                orden.getMontoTotal(),
                orden.getMetodoPago(),
                entradas,
                "Estado actual de la orden: " + orden.getEstado()
        );
    }

    @Transactional
    public void liberarReservasVencidas() {
        List<OrdenCompra> vencidas = ordenRepo.findByEstadoAndFechaExpiracionReservaBefore(
                EstadoOrden.RESERVADA_TEMPORAL, LocalDateTime.now());
        for (OrdenCompra o : vencidas) {
            bloqueoRepo.deleteByOrdenId(o.getId());
            transaccionService.restaurarCuposGenerales(o);
            o.setEstado(EstadoOrden.EXPIRADA);
        }
        ordenRepo.saveAll(vencidas);
        bloqueoRepo.flush();
    }

    private void validar(ReservaRequest req) {
        if (req.clienteId() == null || req.clienteId().isBlank()
                || req.eventoId() == null || req.eventoId().isBlank()
                || req.items() == null || req.items().isEmpty()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST,
                    "Hay que indicar clienteId, eventoId y al menos un item");
        }
        for (ItemReserva item : req.items()) {
            if (item.sectorId() == null || item.sectorId().isBlank()) {
                throw new NegocioException(HttpStatus.BAD_REQUEST,
                        "Cada item de reserva debe indicar sectorId");
            }
            boolean tieneFila = item.fila() != null && !item.fila().isBlank();
            boolean tieneAsiento = item.asiento() != null;
            if (tieneFila != tieneAsiento) {
                throw new NegocioException(HttpStatus.BAD_REQUEST,
                        "Para asientos numerados se debe especificar tanto fila como asiento");
            }
        }
    }

    private boolean esSectorGeneral(EventoDTO evento, ItemReserva item) {
        if (evento.sectores() != null) {
            for (EventoDTO.SectorInfoDTO s : evento.sectores()) {
                if (item.sectorId().equals(s.sectorId())) {
                    return "CAPACIDAD_GENERAL".equalsIgnoreCase(s.tipo());
                }
            }
        }
        return item.fila() == null && item.asiento() == null;
    }

    private int obtenerCapacidadMaxima(EventoDTO evento, String sectorId) {
        if (evento.sectores() != null) {
            for (EventoDTO.SectorInfoDTO s : evento.sectores()) {
                if (sectorId.equals(s.sectorId()) && s.capacidadMaxima() != null && s.capacidadMaxima() > 0) {
                    return s.capacidadMaxima();
                }
            }
        }
        return 1000; // Capacidad por defecto si no está especificada en el mock/evento
    }

    private BigDecimal precioDelSector(EventoDTO evento, String sectorId) {
        if (evento.preciosSectores() != null) {
            return evento.preciosSectores().stream()
                    .filter(p -> sectorId.equals(p.idSector()))
                    .map(EventoDTO.PrecioSectorDTO::precio)
                    .findFirst()
                    .orElseThrow(() -> sinPrecio(sectorId));
        }
        throw sinPrecio(sectorId);
    }

    private NegocioException sinPrecio(String sectorId) {
        return new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                "El sector " + sectorId + " no tiene precio en este evento");
    }

    private String claveAsiento(String eventoId, ItemReserva item) {
        return eventoId + "|" + item.sectorId() + "|" + item.fila() + "|" + item.asiento();
    }

    private String claveGeneral(String eventoId, String sectorId, UUID ordenId, int index) {
        return eventoId + "|" + sectorId + "|GENERAL|" + ordenId + "|" + index;
    }

    private ReservaResponse aRespuesta(OrdenCompra orden) {
        long segundos = Math.max(0,
                Duration.between(LocalDateTime.now(), orden.getFechaExpiracionReserva()).getSeconds());
        return new ReservaResponse(orden.getId(), orden.getEstado(),
                orden.getFechaExpiracionReserva(), orden.getMontoTotal(), segundos);
    }
}
