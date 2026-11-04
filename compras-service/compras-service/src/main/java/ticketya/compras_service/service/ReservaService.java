package ticketya.compras_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ticketya.compras_service.client.EventoDTO;
import ticketya.compras_service.client.EventosClient;
import ticketya.compras_service.dto.ItemReserva;
import ticketya.compras_service.dto.ReservaRequest;
import ticketya.compras_service.dto.ReservaResponse;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.model.*;
import ticketya.compras_service.repository.BloqueoAsientoRepository;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaService {

    private final OrdenCompraRepository ordenRepo;
    private final BloqueoAsientoRepository bloqueoRepo;
    private final EventosClient eventosClient;

    @Value("${compras.reserva.minutos}")
    private int minutosReserva;

    @Value("${compras.limite-entradas-por-cliente}")
    private int limiteEntradas;

    public ReservaService(OrdenCompraRepository ordenRepo,
                          BloqueoAsientoRepository bloqueoRepo,
                          EventosClient eventosClient) {
        this.ordenRepo = ordenRepo;
        this.bloqueoRepo = bloqueoRepo;
        this.eventosClient = eventosClient;
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

        // 2. Las reservas vencidas liberan sus asientos antes de intentar tomarlos
        liberarReservasVencidas();

        // 3. El evento tiene que existir y estar publicado
        EventoDTO evento = eventosClient.obtenerEvento(req.eventoId());
        if (!"PUBLICADO".equals(evento.estado())) {
            throw new NegocioException(HttpStatus.CONFLICT, "El evento no esta publicado");
        }

        // 4. Limite de entradas por cliente para este evento
        long yaTiene = ordenRepo.contarEntradasDelCliente(
                req.clienteId(), req.eventoId(),
                List.of(EstadoOrden.RESERVADA_TEMPORAL, EstadoOrden.COMPLETADA));
        if (yaTiene + req.items().size() > limiteEntradas) {
            throw new NegocioException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Supera el maximo de " + limiteEntradas + " entradas por cliente para este evento");
        }

        // 5. Armamos la orden con su detalle y el monto total
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

        // 6. Un bloqueo por asiento. La restriccion UNICA decide quien gana.
        //    Si falla, se lanza la excepcion y la transaccion deshace todo.
        for (ItemReserva item : req.items()) {
            BloqueoAsiento bloqueo = new BloqueoAsiento();
            bloqueo.setEventoId(req.eventoId());
            bloqueo.setSectorId(item.sectorId());
            bloqueo.setClaveAsiento(claveAsiento(req.eventoId(), item));
            bloqueo.setOrdenId(orden.getId());
            bloqueo.setEstado(EstadoBloqueo.BLOQUEADO);
            bloqueo.setExpiraEn(orden.getFechaExpiracionReserva());
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

    // Marca como EXPIRADAS las ordenes vencidas y libera sus asientos.
    // Despues la va a llamar tambien un proceso programado.
    @Transactional
    public void liberarReservasVencidas() {
        List<OrdenCompra> vencidas = ordenRepo.findByEstadoAndFechaExpiracionReservaBefore(
                EstadoOrden.RESERVADA_TEMPORAL, LocalDateTime.now());
        for (OrdenCompra o : vencidas) {
            bloqueoRepo.deleteByOrdenId(o.getId());
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
            if (item.sectorId() == null || item.fila() == null || item.asiento() == null) {
                throw new NegocioException(HttpStatus.BAD_REQUEST,
                        "Por ahora solo se reservan asientos numerados (sectorId, fila y asiento)");
            }
        }
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

    private ReservaResponse aRespuesta(OrdenCompra orden) {
        long segundos = Math.max(0,
                Duration.between(LocalDateTime.now(), orden.getFechaExpiracionReserva()).getSeconds());
        return new ReservaResponse(orden.getId(), orden.getEstado(),
                orden.getFechaExpiracionReserva(), orden.getMontoTotal(), segundos);
    }
}