package com.ticketya.entradas.service;

import com.ticketya.entradas.client.ComprasClient;
import com.ticketya.entradas.client.EventosClient;
import com.ticketya.entradas.client.PasarelaClient;
import com.ticketya.entradas.dto.EmisionEntradasRequest;
import com.ticketya.entradas.dto.EmisionEntradasResponse;
import com.ticketya.entradas.dto.EntradaResponseDTO;
import com.ticketya.entradas.dto.ItemEntradaEmision;
import com.ticketya.entradas.dto.ValidarQrRequest;
import com.ticketya.entradas.dto.ValidarQrResponse;
import com.ticketya.entradas.model.Entrada;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.model.EstadoSolicitudDevolucion;
import com.ticketya.entradas.model.SolicitudDevolucion;
import com.ticketya.entradas.repository.EntradaRepository;
import com.ticketya.entradas.repository.SolicitudDevolucionRepository;
import com.ticketya.entradas.util.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EntradaService {

    private static final Logger log = LoggerFactory.getLogger(EntradaService.class);

    private final EntradaRepository entradaRepository;
    private final QrCodeGenerator qrCodeGenerator;
    private final SolicitudDevolucionRepository solicitudRepo;
    private final ComprasClient comprasClient;
    private final EventosClient eventosClient;
    private final PasarelaClient pasarelaClient;

    public EntradaService(EntradaRepository entradaRepository, QrCodeGenerator qrCodeGenerator, SolicitudDevolucionRepository solicitudRepo, ComprasClient comprasClient, EventosClient eventosClient, PasarelaClient pasarelaClient) {
        this.entradaRepository = entradaRepository;
        this.qrCodeGenerator = qrCodeGenerator;
        this.solicitudRepo = solicitudRepo;
        this.comprasClient = comprasClient;
        this.eventosClient = eventosClient;
        this.pasarelaClient = pasarelaClient;
    }

    @Transactional
    public EmisionEntradasResponse emitirEntradas(EmisionEntradasRequest request) {
        // Idempotencia: Si ya fueron emitidas para esta orden, devolver las existentes
        List<Entrada> existentes = entradaRepository.findByOrdenCompraId(request.ordenCompraId());
        if (!existentes.isEmpty()) {
            return new EmisionEntradasResponse(
                    request.ordenCompraId(),
                    existentes.stream().map(this::toDTO).toList()
            );
        }

        List<Entrada> entradasAGuardar = new ArrayList<>();
        for (ItemEntradaEmision item : request.asientos()) {
            String qrHash = qrCodeGenerator.generarHashQr(
                    request.eventoId(),
                    request.ordenCompraId(),
                    item.sectorNombre(),
                    item.fila(),
                    item.asiento()
            );

            Entrada entrada = new Entrada();
            entrada.setOrdenCompraId(request.ordenCompraId());
            entrada.setClienteId(request.clienteId());
            entrada.setEventoId(request.eventoId());
            entrada.setSectorNombre(item.sectorNombre());
            entrada.setFila(item.fila());
            entrada.setAsiento(item.asiento());
            entrada.setCodigoQrHash(qrHash);
            entrada.setEstado(EstadoEntrada.EMITIDA);

            entradasAGuardar.add(entrada);
        }

        List<Entrada> guardadas = entradaRepository.saveAll(entradasAGuardar);

        return new EmisionEntradasResponse(
                request.ordenCompraId(),
                guardadas.stream().map(this::toDTO).toList()
        );
    }

    public List<EntradaResponseDTO> obtenerPorOrden(String ordenCompraId) {
        return entradaRepository.findByOrdenCompraId(ordenCompraId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public List<EntradaResponseDTO> obtenerPorCliente(String clienteId) {
        return entradaRepository.findByClienteId(clienteId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private EntradaResponseDTO toDTO(Entrada entrada) {
        return new EntradaResponseDTO(
                entrada.getId(),
                entrada.getOrdenCompraId(),
                entrada.getClienteId(),
                entrada.getEventoId(),
                entrada.getSectorNombre(),
                entrada.getFila(),
                entrada.getAsiento(),
                entrada.getCodigoQrHash(),
                entrada.getEstado(),
                entrada.getFechaEmision()
        );
    }

    //1. SOLICITAR DEVOLUCION (CLIENTE) - VALIDA REGLA 72 HS
    @Transactional
    public SolicitudDevolucion solicitarDevolucion(UUID entradaId, String clienteId, String motivo){
        Entrada entrada = entradaRepository.findById(entradaId)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        if(!entrada.getClienteId().equals(clienteId)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La entrada no pertenece al cliente solicitante");
        }

        if (entrada.getEstado() != EstadoEntrada.EMITIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden devolver entradas en estado EMITIDA");
        }

        // Ya existe una solicitud pendiente?
        if (solicitudRepo.findByEntradaIdAndEstado(entradaId, EstadoSolicitudDevolucion.PENDIENTE).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una solicitud de devolución en trámite");
        }

        // Validar límite de 72 horas antes del inicio del evento
        LocalDateTime fechaInicio = Futures.join(eventosClient.obtenerFechaEvento(entrada.getEventoId()));
        if (fechaInicio != null && LocalDateTime.now().plusHours(72).isAfter(fechaInicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pueden solicitar devoluciones con menos de 72 horas de anticipación al evento");
        }

        SolicitudDevolucion solicitud = new SolicitudDevolucion();
        solicitud.setEntradaId(entradaId);
        solicitud.setClienteId(clienteId);
        solicitud.setMotivo(motivo);
        solicitud.setEstado(EstadoSolicitudDevolucion.PENDIENTE);

        return solicitudRepo.save(solicitud);
    }

    // 2. APROBAR DEVOLUCIÓN (Admin)
    @Transactional
    public void aprobarDevolucion(UUID solicitudId, String respuestaAdmin) {
        SolicitudDevolucion solicitud = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (solicitud.getEstado() != EstadoSolicitudDevolucion.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud ya fue procesada");
        }

        Entrada entrada = entradaRepository.findById(solicitud.getEntradaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada asociada no encontrada"));

        // A. Marcamos la entrada como CANCELADA
        entrada.setEstado(EstadoEntrada.CANCELADA);
        entradaRepository.save(entrada);

        // B. Liberamos el asiento en compras-service
        Futures.join(comprasClient.liberarAsiento(entrada.getEventoId(), entrada.getSectorNombre(), entrada.getFila(), entrada.getAsiento()));

        // C. Solicitamos el reembolso a la pasarela
        Futures.join(pasarelaClient.solicitarReembolso(entrada.getOrdenCompraId(), entrada.getClienteId()));

        // D. Actualizamos la solicitud
        solicitud.setEstado(EstadoSolicitudDevolucion.APROBADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setRespuestaAdmin(respuestaAdmin);
        solicitudRepo.save(solicitud);
    }

    // 3. RECHAZAR DEVOLUCIÓN (Admin)
    @Transactional
    public void rechazarDevolucion(UUID solicitudId, String motivo) {
        SolicitudDevolucion solicitud = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (solicitud.getEstado() != EstadoSolicitudDevolucion.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud ya fue procesada");
        }

        Entrada entrada = entradaRepository.findById(solicitud.getEntradaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada no encontrada"));

        if (entrada.getEstado() != EstadoEntrada.EMITIDA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La entrada no está en estado EMITIDA");
        }

        solicitud.setEstado(EstadoSolicitudDevolucion.RECHAZADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setRespuestaAdmin(motivo != null ? motivo : "Rechazada por administrador");
        solicitudRepo.save(solicitud);
    }

    // 4. CANCELACIÓN MASIVA POR EVENTO CANCELADO CON REINTEGRO IDEMPOTENTE POR ORDEN
    @Transactional
    public int cancelarEntradasPorEvento(String eventoId) {
        List<Entrada> activas = entradaRepository.findByEventoIdAndEstadoNot(eventoId, EstadoEntrada.CANCELADA);
        Set<String> ordenesReintegradas = new HashSet<>();
        for (Entrada entrada : activas) {
            entrada.setEstado(EstadoEntrada.CANCELADA);
            Futures.join(comprasClient.liberarAsiento(entrada.getEventoId(), entrada.getSectorNombre(), entrada.getFila(), entrada.getAsiento()));

            String ordenId = entrada.getOrdenCompraId();
            if (!ordenesReintegradas.contains(ordenId)) {
                try {
                    Futures.join(pasarelaClient.solicitarReembolso(ordenId, entrada.getClienteId()));
                    ordenesReintegradas.add(ordenId);
                } catch (Exception e) {
                    log.error("Error al reintegrar orden {} en cancelacion de evento {}: {}", ordenId, eventoId, e.getMessage());
                }
            }
        }
        entradaRepository.saveAll(activas);
        return activas.size();
    }

    // 5. VALIDACIÓN DE ENTRADAS POR QR
    @Transactional
    public ValidarQrResponse validarQr(ValidarQrRequest request, String validadorId, String roles) {
        Entrada entrada = entradaRepository.findByCodigoQrHash(request.qrHash())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR inexistente"));

        if (request.eventoId() != null && !request.eventoId().isBlank()
                && !request.eventoId().equals(entrada.getEventoId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR de otro evento");
        }

        if (entrada.getEstado() == EstadoEntrada.CANCELADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entrada cancelada");
        }

        if (entrada.getEstado() == EstadoEntrada.USADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Entrada ya utilizada - reingreso denegado");
        }

        // Evento vigente
        try {
            LocalDateTime fechaEvento = Futures.join(eventosClient.obtenerFechaEvento(entrada.getEventoId()));
            if (fechaEvento != null && LocalDateTime.now().isAfter(fechaEvento.plusHours(24))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El evento ya ha finalizado");
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudo verificar fecha de evento {}: {}", entrada.getEventoId(), e.getMessage());
        }

        entrada.setEstado(EstadoEntrada.USADA);
        entrada.setFechaIngreso(LocalDateTime.now());

        try {
            entradaRepository.save(entrada);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Doble escaneo concurrente detectado");
        }

        log.info("Acceso validado para entrada {} por validador {}", entrada.getId(), validadorId);

        return new ValidarQrResponse(
                entrada.getId(),
                entrada.getClienteId(),
                entrada.getEventoId(),
                entrada.getSectorNombre(),
                entrada.getEstado(),
                entrada.getFechaIngreso(),
                "Acceso autorizado"
        );
    }
}
