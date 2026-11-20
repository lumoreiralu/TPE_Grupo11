package ticketya.compras_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ticketya.compras_service.dto.RecaudacionResponse;
import ticketya.compras_service.exception.NegocioException;
import ticketya.compras_service.service.MetricasService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@WebMvcTest(MetricasController.class)
class MetricasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetricasService metricasService;

    @Test
    void recaudacionDevuelve200ConElReporte() throws Exception {
        when(metricasService.obtenerRecaudacion(eq("evento-1"))).thenReturn(new RecaudacionResponse(
                "evento-1",
                new BigDecimal("8500.50"),
                new BigDecimal("9300.50"),
                new BigDecimal("800.00"),
                3L,
                1L));

        mockMvc.perform(get("/api/compras/metricas/{eventoId}/recaudacion", "evento-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventoId").value("evento-1"))
                .andExpect(jsonPath("$.recaudacionTotal").value(8500.50))
                .andExpect(jsonPath("$.recaudacionBruta").value(9300.50))
                .andExpect(jsonPath("$.montoReembolsado").value(800.00))
                .andExpect(jsonPath("$.ordenesConfirmadas").value(3))
                .andExpect(jsonPath("$.ordenesCompensadas").value(1));
    }

    @Test
    void recaudacionPropaga400CuandoElEventoEsInvalido() throws Exception {
        when(metricasService.obtenerRecaudacion(eq(" ")))
                .thenThrow(new NegocioException(BAD_REQUEST, "El identificador del evento es obligatorio"));

        mockMvc.perform(get("/api/compras/metricas/{eventoId}/recaudacion", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El identificador del evento es obligatorio"));
    }
}
