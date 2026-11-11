package ticketya.compras_service.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.model.*;
import ticketya.compras_service.repository.BloqueoAsientoRepository;
import ticketya.compras_service.repository.CupoSectorRepository;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CompraTransaccionService {

    private final OrdenCompraRepository ordenRepo;
    private final BloqueoAsientoRepository bloqueoRepo;
    private final CupoSectorRepository cupoSectorRepo;

    public CompraTransaccionService(OrdenCompraRepository ordenRepo,
                                  BloqueoAsientoRepository bloqueoRepo,
                                  CupoSectorRepository cupoSectorRepo) {
        this.ordenRepo = ordenRepo;
        this.bloqueoRepo = bloqueoRepo;
        this.cupoSectorRepo = cupoSectorRepo;
    }

    @Transactional(readOnly = true)
    public OrdenCompra obtenerOrden(UUID ordenId) {
        return ordenRepo.findById(ordenId)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "La orden " + ordenId + " no existe"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asegurarCupoSector(String eventoId, String sectorId, int capacidadTotal) {
        if (cupoSectorRepo.findByEventoIdAndSectorId(eventoId, sectorId).isEmpty()) {
            try {
                CupoSector cupo = new CupoSector(eventoId, sectorId, capacidadTotal, capacidadTotal);
                cupoSectorRepo.saveAndFlush(cupo);
            } catch (DataIntegrityViolationException ignored) {
                // Ya fue insertado concurrentemente por otra transacción
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrdenCompra iniciarProcesamientoPago(UUID ordenId) {
        OrdenCompra orden = ordenRepo.findById(ordenId)
                .orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "La orden " + ordenId + " no existe"));

        if (orden.getEstado() == EstadoOrden.COMPLETADA || orden.getEstado() == EstadoOrden.PAGO_CONFIRMADO) {
            return orden;
        }

        if (orden.getEstado() == EstadoOrden.PROCESANDO_PAGO) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "La orden ya se encuentra en proceso de confirmación de pago");
        }

        if (orden.getEstado() != EstadoOrden.RESERVADA_TEMPORAL) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "La orden no se encuentra en estado de reserva temporal (estado actual: " + orden.getEstado() + ")");
        }

        // Si venció el tiempo de reserva, se liberan asientos/cupos y se marca expirada de inmediato
        if (LocalDateTime.now().isAfter(orden.getFechaExpiracionReserva())) {
            bloqueoRepo.deleteByOrdenId(ordenId);
            restaurarCuposGenerales(orden);
            orden.setEstado(EstadoOrden.EXPIRADA);
            return ordenRepo.saveAndFlush(orden);
        }

        orden.setEstado(EstadoOrden.PROCESANDO_PAGO);
        return ordenRepo.saveAndFlush(orden);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarPagoRechazado(UUID ordenId, String transaccionId, String metodoPago) {
        bloqueoRepo.deleteByOrdenId(ordenId);
        ordenRepo.findById(ordenId).ifPresent(o -> {
            restaurarCuposGenerales(o);
            o.setEstado(EstadoOrden.CANCELADA);
            o.setMetodoPago(metodoPago);
            o.setTransaccionPagoId(transaccionId);
            o.setFechaExpiracionReserva(LocalDateTime.now());
            ordenRepo.saveAndFlush(o);
        });
        bloqueoRepo.flush();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarPagoAprobado(UUID ordenId, String transaccionId, String metodoPago) {
        ordenRepo.findById(ordenId).ifPresent(o -> {
            o.setEstado(EstadoOrden.PAGO_CONFIRMADO);
            o.setTransaccionPagoId(transaccionId);
            o.setMetodoPago(metodoPago);

            List<BloqueoAsiento> bloqueos = bloqueoRepo.findByOrdenId(ordenId);
            for (BloqueoAsiento b : bloqueos) {
                b.setEstado(EstadoBloqueo.VENDIDO);
            }
            bloqueoRepo.saveAll(bloqueos);
            ordenRepo.saveAndFlush(o);
        });
        bloqueoRepo.flush();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarOrdenCompletada(UUID ordenId) {
        ordenRepo.findById(ordenId).ifPresent(o -> {
            o.setEstado(EstadoOrden.COMPLETADA);
            ordenRepo.saveAndFlush(o);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revertirAEstadoReservado(UUID ordenId) {
        ordenRepo.findById(ordenId).ifPresent(o -> {
            if (o.getEstado() == EstadoOrden.PROCESANDO_PAGO) {
                o.setEstado(EstadoOrden.RESERVADA_TEMPORAL);
                ordenRepo.saveAndFlush(o);
            }
        });
    }

    public void restaurarCuposGenerales(OrdenCompra orden) {
        if (orden.getDetalles() != null) {
            Map<String, Long> generalesPorSector = orden.getDetalles().stream()
                    .filter(d -> d.getFila() == null && d.getAsiento() == null)
                    .collect(Collectors.groupingBy(DetalleReserva::getSectorId, Collectors.counting()));

            generalesPorSector.forEach((sectorId, cantidad) ->
                    cupoSectorRepo.incrementarCupo(orden.getEventoId(), sectorId, cantidad.intValue()));
        }
    }
}
