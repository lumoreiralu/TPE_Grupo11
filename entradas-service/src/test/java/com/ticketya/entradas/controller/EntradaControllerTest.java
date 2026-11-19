package com.ticketya.entradas.controller;

import com.ticketya.entradas.dto.EntradaResponseDTO;
import com.ticketya.entradas.dto.ValidarQrRequest;
import com.ticketya.entradas.dto.ValidarQrResponse;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.model.SolicitudDevolucion;
import com.ticketya.entradas.service.EntradaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntradaController.class)
class EntradaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EntradaService entradaService;

    @Test
    void solicitarDevolucionUsaXUserIdIgnorandoParametroDistinto() throws Exception {
        UUID entradaId = UUID.randomUUID();
        SolicitudDevolucion solicitud = new SolicitudDevolucion();
        when(entradaService.solicitarDevolucion(eq(entradaId), eq("usuario-real"), any()))
                .thenReturn(solicitud);

        mockMvc.perform(post("/api/entradas/{id}/devolucion", entradaId)
                        .header("X-User-Id", "usuario-real")
                        .param("clienteId", "usuario-falso")
                        .param("motivo", "Motivo de prueba"))
                .andExpect(status().isCreated());

        verify(entradaService).solicitarDevolucion(entradaId, "usuario-real", "Motivo de prueba");
    }

    @Test
    void obtenerPorClienteUsaXUserIdIgnorandoPathVariableDistinto() throws Exception {
        EntradaResponseDTO dto = new EntradaResponseDTO(
                UUID.randomUUID(), "orden1", "usuario-real", "evt1", "Campo", "A", 1, "qr1",
                EstadoEntrada.EMITIDA, LocalDateTime.now()
        );
        when(entradaService.obtenerPorCliente("usuario-real")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/entradas/cliente/{clienteId}", "usuario-falso")
                        .header("X-User-Id", "usuario-real"))
                .andExpect(status().isOk());

        verify(entradaService).obtenerPorCliente("usuario-real");
    }

    @Test
    void validarQrPropagaIdentidadYRolDelValidador() throws Exception {
        ValidarQrResponse response = new ValidarQrResponse(
                UUID.randomUUID(), "cliente-1", "evento-1", "Platea",
                EstadoEntrada.USADA, LocalDateTime.now(), "Acceso autorizado"
        );
        when(entradaService.validarQr(any(ValidarQrRequest.class), eq("validador-1"), eq("ROLE_PERSONAL_ACCESO")))
                .thenReturn(response);

        mockMvc.perform(post("/api/entradas/validar-qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrHash\":\"hash-qr\",\"eventoId\":\"evento-1\"}")
                        .header("X-User-Id", "validador-1")
                        .header("X-User-Roles", "ROLE_PERSONAL_ACCESO"))
                .andExpect(status().isOk());

        verify(entradaService).validarQr(
                new ValidarQrRequest("hash-qr", "evento-1"), "validador-1", "ROLE_PERSONAL_ACCESO");
    }
}
