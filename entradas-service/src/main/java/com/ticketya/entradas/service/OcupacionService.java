package com.ticketya.entradas.service;

import com.ticketya.entradas.client.EventoResumenDTO;
import com.ticketya.entradas.client.EventosClient;
import com.ticketya.entradas.dto.OcupacionResponse;
import com.ticketya.entradas.dto.OcupacionSectorDTO;
import com.ticketya.entradas.model.Entrada;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.repository.EntradaRepository;
import com.ticketya.entradas.util.Futures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reporte de ocupación por sector (R12.2).
 *
 * <p>Las entradas son propiedad de este servicio y la capacidad de cada sector
 * vive en eventos-service, por eso el reporte se arma cruzando ambos datos: no
 * se duplica la capacidad en la base de entradas.
 *
 * <p>Reglas: las entradas EMITIDA y USADA cuentan como vendidas, las CANCELADA
 * como devueltas, y los cupos disponibles son la capacidad del sector menos lo
 * vendido (nunca negativo). Un sector sin entradas informa 0 % de ocupación.
 */
@Service
public class OcupacionService {

    private static final Logger log = LoggerFactory.getLogger(OcupacionService.class);
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final int ESCALA_PORCENTAJE = 2;

    private final EntradaRepository entradaRepository;
    private final EventosClient eventosClient;

    public OcupacionService(EntradaRepository entradaRepository, EventosClient eventosClient) {
        this.entradaRepository = entradaRepository;
        this.eventosClient = eventosClient;
    }

    @Transactional(readOnly = true)
    public OcupacionResponse obtenerOcupacion(String eventoId) {
        if (eventoId == null || eventoId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El identificador del evento es obligatorio");
        }

        EventoResumenDTO evento = Futures.join(eventosClient.obtenerEvento(eventoId));
        if (evento == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo obtener la capacidad de los sectores del evento");
        }

        Map<String, ConteoSector> conteos = contarPorSector(entradaRepository.findByEventoId(eventoId));

        List<OcupacionSectorDTO> sectores = new ArrayList<>();
        Set<String> clavesInformadas = new HashSet<>();
        for (EventoResumenDTO.SectorDTO sector : evento.sectoresSeguros()) {
            String claveNombre = claveSector(sector.nombre());
            String claveId = claveSector(sector.sectorId());
            // Las entradas guardan el nombre del sector; se acepta también el
            // identificador para datos cargados con el id en lugar del nombre.
            ConteoSector conteo = conteos.get(claveNombre);
            if (conteo == null) {
                conteo = conteos.get(claveId);
            }
            if (conteo == null) {
                conteo = ConteoSector.vacio(nombreVisible(sector));
            }
            sectores.add(armar(sector.sectorId(), nombreVisible(sector), sector.capacidadMaxima(), conteo));
            clavesInformadas.add(claveNombre);
            clavesInformadas.add(claveId);
        }

        // Entradas de sectores que ya no figuran en el catálogo: se informan
        // para que el total del evento no pierda cupos vendidos o devueltos.
        conteos.forEach((clave, conteo) -> {
            if (!clavesInformadas.contains(clave)) {
                sectores.add(armar(null, conteo.nombre(), null, conteo));
            }
        });

        long totalVendidas = sectores.stream().mapToLong(OcupacionSectorDTO::vendidas).sum();
        long totalDisponibles = sectores.stream().mapToLong(OcupacionSectorDTO::disponibles).sum();
        long totalDevueltas = sectores.stream().mapToLong(OcupacionSectorDTO::devueltas).sum();
        long capacidadConocida = sectores.stream()
                .filter(sector -> sector.capacidad() != null)
                .mapToLong(OcupacionSectorDTO::capacidad)
                .sum();

        log.debug("Ocupación evento {}: sectores={} vendidas={} disponibles={} devueltas={}",
                eventoId, sectores.size(), totalVendidas, totalDisponibles, totalDevueltas);

        return new OcupacionResponse(
                eventoId,
                evento.nombre(),
                sectores,
                totalVendidas,
                totalDisponibles,
                totalDevueltas,
                porcentaje(totalVendidas, capacidadConocida));
    }

    private Map<String, ConteoSector> contarPorSector(List<Entrada> entradas) {
        Map<String, ConteoSector> conteos = new LinkedHashMap<>();
        for (Entrada entrada : entradas) {
            ConteoSector conteo = conteos.computeIfAbsent(
                    claveSector(entrada.getSectorNombre()),
                    clave -> new ConteoSector(entrada.getSectorNombre()));
            if (entrada.getEstado() == EstadoEntrada.CANCELADA) {
                conteo.devueltas++;
            } else {
                conteo.vendidas++;
            }
        }
        return conteos;
    }

    private OcupacionSectorDTO armar(String sectorId, String nombre, Integer capacidad, ConteoSector conteo) {
        Long capacidadSector = capacidad != null ? capacidad.longValue() : null;
        long disponibles = capacidad != null ? Math.max(0, capacidad - conteo.vendidas) : 0L;
        return new OcupacionSectorDTO(
                sectorId,
                nombre,
                capacidad,
                conteo.vendidas,
                disponibles,
                conteo.devueltas,
                porcentaje(conteo.vendidas, capacidadSector));
    }

    /**
     * Porcentaje (0–100) de cupos vendidos sobre la capacidad del sector.
     * Sin capacidad declarada o sin entradas el resultado es 0 %.
     */
    private BigDecimal porcentaje(long vendidas, Long capacidad) {
        if (capacidad == null || capacidad <= 0) {
            return BigDecimal.ZERO.setScale(ESCALA_PORCENTAJE, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(vendidas)
                .multiply(CIEN)
                .divide(BigDecimal.valueOf(capacidad), ESCALA_PORCENTAJE, RoundingMode.HALF_UP);
    }

    private String claveSector(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private String nombreVisible(EventoResumenDTO.SectorDTO sector) {
        return sector.nombre() != null && !sector.nombre().isBlank() ? sector.nombre() : sector.sectorId();
    }

    private static final class ConteoSector {
        private final String nombre;
        private long vendidas;
        private long devueltas;

        private ConteoSector(String nombre) {
            this.nombre = nombre;
        }

        private static ConteoSector vacio(String nombre) {
            return new ConteoSector(nombre);
        }

        private String nombre() {
            return nombre;
        }
    }
}
