package com.ticketya.entradas;

import com.ticketya.entradas.client.ComprasClient;
import com.ticketya.entradas.client.ComprasClient.OrdenInfo;
import com.ticketya.entradas.client.EventosClient;
import com.ticketya.entradas.client.PasarelaClient;
import com.ticketya.entradas.messaging.DomainEvent;
import com.ticketya.entradas.messaging.DomainEventPublisher;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DevolucionServiceTest {

    @Mock private EntradaRepository entradaRepository;
    @Mock private QrCodeGenerator qrCodeGenerator;
    @Mock private SolicitudDevolucionRepository solicitudRepo;
    @Mock private ComprasClient comprasClient;
    @Mock private EventosClient eventosClient;
    @Mock private PasarelaClient pasarelaClient;
    @Mock private DomainEventPublisher eventPublisher;

    private EntradaService entradaService;

    @BeforeEach
    void setUp() {
        entradaService = new EntradaService(
                entradaRepository, qrCodeGenerator, solicitudRepo,
                comprasClient, eventosClient, pasarelaClient, eventPublisher);
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
        when(eventosClient.obtenerFechaEvento("EVT-1"))
                .thenReturn(CompletableFuture.completedFuture(LocalDateTime.now().plusHours(24)));
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
        when(eventosClient.obtenerFechaEvento("EVT-1"))
                .thenReturn(CompletableFuture.completedFuture(LocalDateTime.now().plusDays(5)));
        when(solicitudRepo.save(any(SolicitudDevolucion.class))).thenAnswer(i -> i.getArgument(0));

        SolicitudDevolucion resultado = entradaService.solicitarDevolucion(entradaId, "CLI-123", "Viaje de trabajo");
        assertNotNull(resultado);
        assertEquals(EstadoSolicitudDevolucion.PENDIENTE, resultado.getEstado());
    }

    @Test
    @DisplayName("Rechazar devolución conserva la entrada EMITIDA y no reintegra")
    void testRechazarDevolucionConservaEntradaEmitida() {
        UUID solicitudId = UUID.randomUUID();
        UUID entradaId = UUID.randomUUID();
        SolicitudDevolucion solicitud = new SolicitudDevolucion();
        solicitud.setId(solicitudId);
        solicitud.setEntradaId(entradaId);
        solicitud.setEstado(EstadoSolicitudDevolucion.PENDIENTE);

        Entrada entrada = new Entrada();
        entrada.setId(entradaId);
        entrada.setEstado(EstadoEntrada.EMITIDA);

        when(solicitudRepo.findById(solicitudId)).thenReturn(Optional.of(solicitud));
        when(entradaRepository.findById(entradaId)).thenReturn(Optional.of(entrada));
        when(solicitudRepo.save(any(SolicitudDevolucion.class))).thenAnswer(i -> i.getArgument(0));

        entradaService.rechazarDevolucion(solicitudId, "Fuera de plazo");

        assertEquals(EstadoSolicitudDevolucion.RECHAZADA, solicitud.getEstado());
        assertEquals("Fuera de plazo", solicitud.getRespuestaAdmin());
        assertEquals(EstadoEntrada.EMITIDA, entrada.getEstado());
        assertNotNull(solicitud.getFechaResolucion());
        verifyNoInteractions(pasarelaClient);
        verify(comprasClient, never()).liberarAsiento(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Aprobar devolución debe cancelar la entrada, liberar asiento, reintegrar y notificar")
    void testAprobarDevolucionCompleto() {
        UUID solicitudId = UUID.randomUUID();
        UUID entradaId = UUID.randomUUID();
        SolicitudDevolucion solicitud = new SolicitudDevolucion();
        solicitud.setId(solicitudId);
        solicitud.setEntradaId(entradaId);
        solicitud.setEstado(EstadoSolicitudDevolucion.PENDIENTE);

        Entrada entrada = new Entrada();
        entrada.setId(entradaId);
        entrada.setOrdenCompraId("ORD-99");
        entrada.setClienteId("CLI-123");
        entrada.setEventoId("EVT-1");
        entrada.setSectorNombre("Platea");
        entrada.setFila("1");
        entrada.setAsiento(5);
        entrada.setEstado(EstadoEntrada.EMITIDA);

        BigDecimal monto = new BigDecimal("150.00");
        when(solicitudRepo.findById(solicitudId)).thenReturn(Optional.of(solicitud));
        when(entradaRepository.findById(entradaId)).thenReturn(Optional.of(entrada));
        when(comprasClient.liberarAsiento("EVT-1", "Platea", "1", 5))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(comprasClient.obtenerOrden("ORD-99"))
                .thenReturn(CompletableFuture.completedFuture(new OrdenInfo("TX-99", monto)));
        when(pasarelaClient.solicitarReembolso("TX-99", monto, "devolucion:ORD-99"))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(solicitudRepo.save(any(SolicitudDevolucion.class))).thenAnswer(i -> i.getArgument(0));

        entradaService.aprobarDevolucion(solicitudId, "Aprobado ok");

        assertEquals(EstadoEntrada.CANCELADA, entrada.getEstado());
        assertEquals(EstadoSolicitudDevolucion.APROBADA, solicitud.getEstado());
        verify(comprasClient).liberarAsiento("EVT-1", "Platea", "1", 5);
        verify(pasarelaClient).solicitarReembolso("TX-99", monto, "devolucion:ORD-99");

        ArgumentCaptor<DomainEvent> evento = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(evento.capture());
        assertEquals("devolucion.aprobada", evento.getValue().type());
        assertEquals("ORD-99", evento.getValue().aggregateId());
    }

    @Test
    @DisplayName("Cancelación masiva reintegra una sola vez por orden e ignora duplicados")
    void testCancelacionMasivaReintegraUnaVezPorOrden() {
        Entrada entrada1 = entrada("ORD-A", "CLI-1", "EVT-1", "Platea", "1", 1);
        Entrada entrada2 = entrada("ORD-A", "CLI-1", "EVT-1", "Platea", "1", 2);
        Entrada entrada3 = entrada("ORD-B", "CLI-2", "EVT-1", "Platea", "1", 3);

        when(entradaRepository.findByEventoIdAndEstadoNot("EVT-1", EstadoEntrada.CANCELADA))
                .thenReturn(List.of(entrada1, entrada2, entrada3));
        when(comprasClient.liberarAsiento(anyString(), anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(comprasClient.obtenerOrden("ORD-A"))
                .thenReturn(CompletableFuture.completedFuture(new OrdenInfo("TX-A", new BigDecimal("100.00"))));
        when(comprasClient.obtenerOrden("ORD-B"))
                .thenReturn(CompletableFuture.completedFuture(new OrdenInfo("TX-B", new BigDecimal("200.00"))));
        when(pasarelaClient.solicitarReembolso(anyString(), any(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        int canceladas = entradaService.cancelarEntradasPorEvento("EVT-1");

        assertEquals(3, canceladas);
        assertTrue(List.of(entrada1, entrada2, entrada3).stream()
                .allMatch(e -> e.getEstado() == EstadoEntrada.CANCELADA));

        // Una sola llamada de reintegro por orden, con clave determinística eventoId + ordenId
        verify(pasarelaClient, times(1)).solicitarReembolso("TX-A", new BigDecimal("100.00"), "EVT-1:ORD-A");
        verify(pasarelaClient, times(1)).solicitarReembolso("TX-B", new BigDecimal("200.00"), "EVT-1:ORD-B");

        ArgumentCaptor<DomainEvent> eventos = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publish(eventos.capture());
        assertTrue(eventos.getAllValues().stream()
                .allMatch(e -> "pago.compensado".equals(e.type())));
    }

    @Test
    @DisplayName("Cancelación masiva repetida no vuelve a reintegrar una orden ya cancelada")
    void testCancelacionMasivaRepetidaNoReintegraDeNuevo() {
        Entrada entrada = entrada("ORD-A", "CLI-1", "EVT-1", "Platea", "1", 1);

        // En la segunda ejecución la entrada ya está CANCELADA y no aparece entre las activas
        when(entradaRepository.findByEventoIdAndEstadoNot("EVT-1", EstadoEntrada.CANCELADA))
                .thenReturn(List.of());

        int canceladas = entradaService.cancelarEntradasPorEvento("EVT-1");

        assertEquals(0, canceladas);
        verifyNoInteractions(pasarelaClient);
        verify(eventPublisher, never()).publish(any());
    }

    private Entrada entrada(String ordenId, String clienteId, String eventoId,
                            String sector, String fila, Integer asiento) {
        Entrada entrada = new Entrada();
        entrada.setId(UUID.randomUUID());
        entrada.setOrdenCompraId(ordenId);
        entrada.setClienteId(clienteId);
        entrada.setEventoId(eventoId);
        entrada.setSectorNombre(sector);
        entrada.setFila(fila);
        entrada.setAsiento(asiento);
        entrada.setEstado(EstadoEntrada.EMITIDA);
        return entrada;
    }
}
