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
import ticketya.compras_service.util.Futures;

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
    private final EntradasClient entradasClient;
    private final CompraTransaccionService transaccionService;
    private final CompraSagaService compraSagaService;
    private final FilaVirtualService filaVirtualService;

    @Value("${compras.reserva.minutos}")
    private int minutosReserva;

    @Value("${compras.limite-entradas-por-cliente}")
    private int limiteEntradas;

    public ReservaService(OrdenCompraRepository ordenRepo,
                          BloqueoAsientoRepository bloqueoRepo,
                          CupoSectorRepository cupoSectorRepo,
                          EventosClient eventosClient,
                          EntradasClient entradasClient,
                          CompraTransaccionService transaccionService,
                          CompraSagaService compraSagaService,
                          FilaVirtualService filaVirtualService) {
        this.ordenRepo = ordenRepo;
        this.bloqueoRepo = bloqueoRepo;
        this.cupoSectorRepo = cupoSectorRepo;
        this.eventosClient = eventosClient;
        this.entradasClient = entradasClient;
        this.transaccionService = transaccionService;
        this.compraSagaService = compraSagaService;
        this.filaVirtualService = filaVirtualService;
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
        EventoDTO evento = Futures.join(eventosClient.obtenerEvento(req.eventoId()));
        if (!"PUBLICADO".equals(evento.estado())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El evento no esta publicado");
        }

        // 3.1. Fila virtual: en alta demanda se exige pase vigente antes de crear la orden
        filaVirtualService.exigirPaseParaReserva(evento, req.clienteId(), req.paseId());

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

        filaVirtualService.consumirPase(req.paseId(), req.eventoId(), req.clienteId());
        return aRespuesta(orden);
    }

    // Confirmación de pago con límites transaccionales independientes (sin llamadas HTTP dentro de transacciones de BD)
    public ConfirmacionPagoResponse confirmarPago(UUID ordenId, ConfirmarPagoRequest req) {
        return compraSagaService.confirmarPago(ordenId, req);
    }

    public ConfirmacionPagoResponse consultarOrden(UUID ordenId) {
        OrdenCompra orden = transaccionService.obtenerOrden(ordenId);
        List<EmisionResponse.EntradaDTO> entradas = List.of();
        if (orden.getEstado() == EstadoOrden.COMPLETADA || orden.getEstado() == EstadoOrden.PAGO_CONFIRMADO) {
            entradas = Futures.join(entradasClient.obtenerEntradasPorOrden(orden.getId().toString()));
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
