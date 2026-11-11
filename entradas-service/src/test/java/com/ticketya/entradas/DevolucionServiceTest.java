package com.ticketya.entradas;

import com.ticketya.entradas.client.ComprasClient;
import com.ticketya.entradas.client.EventosClient;
import com.ticketya.entradas.client.PasarelaClient;
import com.ticketya.entradas.model.Entrada;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.model.EstadoSolicitudDevolucion;
import com.ticketya.entradas.model.SolicitudDevolucion;
import com.ticketya.entradas.repository.EntradaRepository;
import com.ticketya.entradas.repository.SolicitudDevolucionRepository;
import com.ticketya.entradas.service.EntradaService;
import com.ticketya.entradas.service.QrCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DevolucionServiceTest {

    @Mock private EntradaRepository entradaRepository;
    @Mock private QrCodeGenerator qrCodeGenerator;
    @Mock private SolicitudDevolucionRepository solicitudRepo;
    @Mock private ComprasClient comprasClient;
    @Mock private EventosClient eventosClient;
    @Mock private PasarelaClient pasarelaClient;

    private EntradaService entradaService;
    @BeforeEach void setUp() {
        entradaService = new EntradaService(
                entradaRepository, qrCodeGenerator, solicitudRepo,
                comprasClient, eventosClient, pasarelaClient );
    }

    @Test
    @DisplayName("Debe rechazar la devolución si faltan menos de 72 horas para el evento")
    void testRechazoMenosDe72Horas() {
        UUID entradaId = UUID.randomUUID();
        Entrada entrada = new Entrada();
        entrada.setId(entradaId);
        entrada.setClienteId("CLI-123");
        entrada.setEventoId("EVT-1");
        entrada.setEstado(EstadoEntrada.EMITIDA);

        when(entradaRepository.findById(entradaId)).thenReturn(Optional.of(entrada));
        // Evento empieza en 24 horas (menos de 72 hs)
        when(eventosClient.obtenerFechaEvento("EVT-1")).thenReturn(LocalDateTime.now().plusHours(24));
        assertThrows(ResponseStatusException.class, () -> entradaService.solicitarDevolucion(entradaId, "CLI-123", "Imprevisto laboral")
        );
    }

    @Test
    @DisplayName("Debe aceptar la solicitud si faltan más de 72 horas")
    void testAceptaDevolucionMasDe72Horas() {
        UUID entradaId = UUID.randomUUID();
        Entrada entrada = new Entrada();
        entrada.setId(entradaId);
        entrada.setClienteId("CLI-123");
        entrada.setEventoId("EVT-1");
        entrada.setEstado(EstadoEntrada.EMITIDA);

        when(entradaRepository.findById(entradaId)).thenReturn(Optional.of(entrada));
        // Evento empieza en 5 días (más de 72 hs)
        when(eventosClient.obtenerFechaEvento("EVT-1")).thenReturn(LocalDateTime.now().plusDays(5));
        when(solicitudRepo.save(any(SolicitudDevolucion.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudDevolucion resultado = entradaService.solicitarDevolucion(entradaId, "CLI-123", "Viaje de trabajo");
        assertNotNull(resultado);
        assertEquals(EstadoSolicitudDevolucion.PENDIENTE, resultado.getEstado());
    }

    @Test
    @DisplayName("Aprobar devolución debe cancelar la entrada, liberar asiento y solicitar reintegro")
    void testAprobarDevolucionCompleto() {
        UUID solicitudId = UUID.randomUUID();
        UUID entradaId = UUID.randomUUID();
        SolicitudDevolucion solicitud = new SolicitudDevolucion();
        solicitud.setId(solicitudId);
        solicitud.setEntradaId(entradaId);
        solicitud.setEstado(EstadoSolicitudDevolucion.PENDIENTE);

        Entrada entrada = new Entrada(); entrada.setId(entradaId);
        entrada.setOrdenCompraId("ORD-99");
        entrada.setClienteId("CLI-123");
        entrada.setEventoId("EVT-1");
        entrada.setSectorNombre("Platea");
        entrada.setFila("1");
        entrada.setAsiento(5);
        entrada.setEstado(EstadoEntrada.EMITIDA);

        when(solicitudRepo.findById(solicitudId)).thenReturn(Optional.of(solicitud));
        when(entradaRepository.findById(entradaId)).thenReturn(Optional.of(entrada));

        entradaService.aprobarDevolucion(solicitudId, "Aprobado ok");
        assertEquals(EstadoEntrada.CANCELADA, entrada.getEstado());
        assertEquals(EstadoSolicitudDevolucion.APROBADA, solicitud.getEstado());
        verify(comprasClient).liberarAsiento("EVT-1", "Platea", "1", 5);
        verify(pasarelaClient).solicitarReembolso("ORD-99", "CLI-123");
    }

}


