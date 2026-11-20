package com.ticketya.entradas.service;

import com.ticketya.entradas.client.EventoResumenDTO;
import com.ticketya.entradas.client.EventosClient;
import com.ticketya.entradas.dto.OcupacionResponse;
import com.ticketya.entradas.dto.OcupacionSectorDTO;
import com.ticketya.entradas.model.Entrada;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.repository.EntradaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Verifica el reporte de ocupación por sector (R12.2) contra la base real (H2)
 * y con eventos-service simulado, que es el dueño de la capacidad.
 */
@SpringBootTest
class OcupacionServiceTest {

    private static final String EVENTO = "evento-ocupacion";

    @Autowired
    private OcupacionService ocupacionService;

    @Autowired
    private EntradaRepository entradaRepository;

    @MockitoBean
    private EventosClient eventosClient;

    @BeforeEach
    void setUp() {
        entradaRepository.deleteAll();
        when(eventosClient.obtenerEvento(anyString())).thenReturn(CompletableFuture.completedFuture(
                new EventoResumenDTO(EVENTO, "Recital de prueba", List.of(
                        new EventoResumenDTO.SectorDTO("sector-platea", "Platea", 100),
                        new EventoResumenDTO.SectorDTO("sector-campo", "Campo", 50),
                        new EventoResumenDTO.SectorDTO("sector-vip", "VIP", 20)))));
    }

    @Test
    void ocupacionPorSectorSumaElTotalDeEntradas() {
        guardarEntrada("Platea", EstadoEntrada.EMITIDA);
        guardarEntrada("Platea", EstadoEntrada.USADA);
        guardarEntrada("Platea", EstadoEntrada.EMITIDA);
        guardarEntrada("Platea", EstadoEntrada.CANCELADA);
        guardarEntrada("Campo", EstadoEntrada.EMITIDA);
        guardarEntrada("Campo", EstadoEntrada.EMITIDA);

        OcupacionResponse ocupacion = ocupacionService.obtenerOcupacion(EVENTO);

        assertEquals(EVENTO, ocupacion.eventoId());
        assertEquals("Recital de prueba", ocupacion.nombreEvento());
        assertEquals(3, ocupacion.sectores().size());

        OcupacionSectorDTO platea = buscarSector(ocupacion, "Platea");
        assertEquals("sector-platea", platea.sectorId());
        assertEquals(100, platea.capacidad());
        assertEquals(3L, platea.vendidas());
        assertEquals(97L, platea.disponibles());
        assertEquals(1L, platea.devueltas());
        assertEquals(new BigDecimal("3.00"), platea.porcentajeOcupacion());

        OcupacionSectorDTO campo = buscarSector(ocupacion, "Campo");
        assertEquals(2L, campo.vendidas());
        assertEquals(48L, campo.disponibles());
        assertEquals(0L, campo.devueltas());
        assertEquals(new BigDecimal("4.00"), campo.porcentajeOcupacion());

        // El total del evento es la suma de los sectores.
        assertEquals(5L, ocupacion.totalVendidas());
        assertEquals(165L, ocupacion.totalDisponibles());
        assertEquals(1L, ocupacion.totalDevueltas());
        assertEquals(new BigDecimal("2.94"), ocupacion.porcentajeOcupacionTotal());
    }

    @Test
    void sectorSinEntradasDevuelveCeroPorCiento() {
        guardarEntrada("Platea", EstadoEntrada.EMITIDA);

        OcupacionResponse ocupacion = ocupacionService.obtenerOcupacion(EVENTO);

        OcupacionSectorDTO vip = buscarSector(ocupacion, "VIP");
        assertEquals(0L, vip.vendidas());
        assertEquals(20L, vip.disponibles());
        assertEquals(0L, vip.devueltas());
        assertEquals(new BigDecimal("0.00"), vip.porcentajeOcupacion());
    }

    @Test
    void eventoInexistenteSePropagaComo404() {
        when(eventosClient.obtenerEvento(eq("evento-inexistente")))
                .thenReturn(CompletableFuture.failedFuture(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado")));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> ocupacionService.obtenerOcupacion("evento-inexistente"));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void eventoVacioSeRechazaCon400() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> ocupacionService.obtenerOcupacion(" "));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void eventosServiceCaidoDevuelve503EnLugarDeOcupacionInventada() {
        when(eventosClient.obtenerEvento(eq("evento-caido")))
                .thenReturn(CompletableFuture.failedFuture(new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "eventos-service no responde")));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> ocupacionService.obtenerOcupacion("evento-caido"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void entradasDeSectoresNoDeclaradosSeInformanIgual() {
        guardarEntrada("Platea", EstadoEntrada.EMITIDA);
        guardarEntrada("Sector removido", EstadoEntrada.CANCELADA);

        OcupacionResponse ocupacion = ocupacionService.obtenerOcupacion(EVENTO);

        assertNotNull(buscarSector(ocupacion, "Sector removido"));
        assertEquals(1L, ocupacion.totalDevueltas());
    }

    private OcupacionSectorDTO buscarSector(OcupacionResponse ocupacion, String sector) {
        return ocupacion.sectores().stream()
                .filter(item -> sector.equals(item.sector()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se informó el sector " + sector));
    }

    private void guardarEntrada(String sectorNombre, EstadoEntrada estado) {
        Entrada entrada = new Entrada();
        entrada.setOrdenCompraId("orden-" + sectorNombre + "-" + estado);
        entrada.setClienteId("cliente-1");
        entrada.setEventoId(EVENTO);
        entrada.setSectorNombre(sectorNombre);
        entrada.setCodigoQrHash("hash-" + sectorNombre + "-" + estado + "-" + System.nanoTime());
        entrada.setEstado(estado);
        entradaRepository.saveAndFlush(entrada);
    }
}
