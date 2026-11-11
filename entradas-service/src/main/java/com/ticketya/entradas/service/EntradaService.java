package com.ticketya.entradas.service;

import com.ticketya.entradas.client.ComprasClient;
import com.ticketya.entradas.client.EventosClient;
import com.ticketya.entradas.client.PasarelaClient;
import com.ticketya.entradas.dto.EmisionEntradasRequest;
import com.ticketya.entradas.dto.EmisionEntradasResponse;
import com.ticketya.entradas.dto.EntradaResponseDTO;
import com.ticketya.entradas.dto.ItemEntradaEmision;
import com.ticketya.entradas.model.Entrada;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.model.EstadoSolicitudDevolucion;
import com.ticketya.entradas.model.SolicitudDevolucion;
import com.ticketya.entradas.repository.EntradaRepository;
import com.ticketya.entradas.repository.SolicitudDevolucionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EntradaService {

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
        LocalDateTime fechaInicio = eventosClient.obtenerFechaEvento(entrada.getEventoId());
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
        comprasClient.liberarAsiento(entrada.getEventoId(), entrada.getSectorNombre(), entrada.getFila(), entrada.getAsiento());

        // C. Solicitamos el reembolso a la pasarela
        pasarelaClient.solicitarReembolso(entrada.getOrdenCompraId(), entrada.getClienteId());

        // D. Actualizamos la solicitud
        solicitud.setEstado(EstadoSolicitudDevolucion.APROBADA);
        solicitud.setFechaResolucion(LocalDateTime.now());
        solicitud.setRespuestaAdmin(respuestaAdmin);
        solicitudRepo.save(solicitud);
    }


    // 3. CANCELACIÓN MASIVA POR EVENTO CANCELADO
    @Transactional
    public int cancelarEntradasPorEvento(String eventoId) {
        List<Entrada> activas = entradaRepository.findByEventoIdAndEstadoNot(eventoId, EstadoEntrada.CANCELADA);
        for (Entrada entrada : activas) {
            entrada.setEstado(EstadoEntrada.CANCELADA);
            comprasClient.liberarAsiento(entrada.getEventoId(), entrada.getSectorNombre(), entrada.getFila(), entrada.getAsiento());
        }
        entradaRepository.saveAll(activas);
        return activas.size();
    }

}

