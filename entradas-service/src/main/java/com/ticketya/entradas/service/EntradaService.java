package com.ticketya.entradas.service;

import com.ticketya.entradas.dto.EmisionEntradasRequest;
import com.ticketya.entradas.dto.EmisionEntradasResponse;
import com.ticketya.entradas.dto.EntradaResponseDTO;
import com.ticketya.entradas.dto.ItemEntradaEmision;
import com.ticketya.entradas.model.Entrada;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.repository.EntradaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EntradaService {

    private final EntradaRepository entradaRepository;
    private final QrCodeGenerator qrCodeGenerator;

    public EntradaService(EntradaRepository entradaRepository, QrCodeGenerator qrCodeGenerator) {
        this.entradaRepository = entradaRepository;
        this.qrCodeGenerator = qrCodeGenerator;
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
}

