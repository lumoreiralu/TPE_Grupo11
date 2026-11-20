package com.ticketya.entradas.controller;

import com.ticketya.entradas.dto.OcupacionResponse;
import com.ticketya.entradas.dto.OcupacionSectorDTO;
import com.ticketya.entradas.service.OcupacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetricasController.class)
class MetricasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OcupacionService ocupacionService;

    @Test
    void ocupacionDevuelveElDesglosePorSector() throws Exception {
        when(ocupacionService.obtenerOcupacion(eq("evento-1"))).thenReturn(new OcupacionResponse(
                "evento-1",
                "Recital",
                List.of(
                        new OcupacionSectorDTO("sector-platea", "Platea", 100, 3L, 97L, 1L,
                                new BigDecimal("3.00")),
                        new OcupacionSectorDTO("sector-vip", "VIP", 20, 0L, 20L, 0L,
                                new BigDecimal("0.00"))),
                3L,
                117L,
                1L,
                new BigDecimal("2.50")));

        mockMvc.perform(get("/api/entradas/metricas/{eventoId}/ocupacion", "evento-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value("evento-1"))
                .andExpect(jsonPath("$.nombreEvento").value("Recital"))
                .andExpect(jsonPath("$.sectores[0].sector").value("Platea"))
                .andExpect(jsonPath("$.sectores[0].vendidas").value(3))
                .andExpect(jsonPath("$.sectores[0].disponibles").value(97))
                .andExpect(jsonPath("$.sectores[0].devueltas").value(1))
                .andExpect(jsonPath("$.sectores[0].porcentajeOcupacion").value(3.00))
                .andExpect(jsonPath("$.sectores[1].porcentajeOcupacion").value(0.00))
                .andExpect(jsonPath("$.totalVendidas").value(3))
                .andExpect(jsonPath("$.totalDisponibles").value(117))
                .andExpect(jsonPath("$.totalDevueltas").value(1));
    }

    @Test
    void ocupacionPropaga404CuandoElEventoNoExiste() throws Exception {
        when(ocupacionService.obtenerOcupacion(eq("evento-inexistente")))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));

        mockMvc.perform(get("/api/entradas/metricas/{eventoId}/ocupacion", "evento-inexistente"))
                .andExpect(status().isNotFound());
    }
}
