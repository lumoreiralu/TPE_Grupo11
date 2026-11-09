package ticketya.compras_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ticketya.compras_service.client.EmisionResponse.EntradaDTO;
import ticketya.compras_service.dto.ConfirmacionPagoResponse;
import ticketya.compras_service.dto.ReservaResponse;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.service.ReservaService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompraController.class)
class CompraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservaService reservaService;

    @Test
    void reservarDevuelve201YPasaLaClaveDeIdempotencia() throws Exception {
        ReservaResponse respuesta = new ReservaResponse(
                UUID.randomUUID(),
                EstadoOrden.RESERVADA_TEMPORAL,
                LocalDateTime.now().plusMinutes(10),
                new BigDecimal("100.00"),
                600);
        when(reservaService.reservar(any(), eq("idem-1"))).thenReturn(respuesta);

        mockMvc.perform(post("/api/compras/reservar")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": "cliente-1",
                                  "eventoId": "evt-1",
                                  "items": [{"sectorId": "sector-1", "fila": "A", "asiento": 1}]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RESERVADA_TEMPORAL"))
                .andExpect(jsonPath("$.segundosRestantes").value(600));
    }

    @Test
    void confirmarPagoDevuelve200ConEntradas() throws Exception {
        UUID ordenId = UUID.randomUUID();
        ConfirmacionPagoResponse respuesta = new ConfirmacionPagoResponse(
                ordenId,
                EstadoOrden.COMPLETADA,
                "txn-999",
                new BigDecimal("100.00"),
                "TARJETA",
                List.of(new EntradaDTO("ent-1", "sector-1", "A", 1, "qr-hash-1", "EMITIDA", LocalDateTime.now().toString())),
                "Compra completada exitosamente"
        );
        when(reservaService.confirmarPago(eq(ordenId), any())).thenReturn(respuesta);

        mockMvc.perform(post("/api/compras/{id}/confirmar-pago", ordenId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metodoPago": "TARJETA"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.transaccionPagoId").value("txn-999"))
                .andExpect(jsonPath("$.entradas[0].codigoQrHash").value("qr-hash-1"));
    }

    @Test
    void consultarOrdenDevuelve200ConEstadoYDetalles() throws Exception {
        UUID ordenId = UUID.randomUUID();
        ConfirmacionPagoResponse respuesta = new ConfirmacionPagoResponse(
                ordenId,
                EstadoOrden.COMPLETADA,
                "txn-999",
                new BigDecimal("100.00"),
                "TARJETA",
                List.of(new EntradaDTO("ent-1", "sector-1", "A", 1, "qr-hash-1", "EMITIDA", LocalDateTime.now().toString())),
                "Estado actual de la orden: COMPLETADA"
        );
        when(reservaService.consultarOrden(eq(ordenId))).thenReturn(respuesta);

        mockMvc.perform(get("/api/compras/{id}", ordenId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordenId").value(ordenId.toString()))
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));
    }
}
