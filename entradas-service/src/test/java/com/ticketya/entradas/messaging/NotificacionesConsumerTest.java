package com.ticketya.entradas.messaging;

import com.ticketya.entradas.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class NotificacionesConsumerTest {

    @Autowired
    private NotificacionesConsumer consumer;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @Autowired
    private List<Binding> bindings;

    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    @Test
    void colaDeNotificacionesEstaSuscritaALosEventosDelDominio() {
        Set<String> routingKeys = bindings.stream()
                .filter(binding -> "notificaciones.q".equals(binding.getDestination()))
                .map(Binding::getRoutingKey)
                .collect(Collectors.toSet());

        assertTrue(routingKeys.containsAll(Set.of(
                "pago.aprobado",
                "devolucion.aprobada",
                "evento.cancelado",
                "evento.modificado")));
    }

    @Test
    void pagoAprobadoEnviaConfirmacionAlCliente() {
        DomainEvent event = evento("evt-pago", "pago.aprobado", "orden-1", Map.of(
                "ordenId", "orden-1",
                "clienteId", "cliente1",
                "email", "cliente@ticketya.com"));

        consumer.handleNotificacion(event, "corr-pago");

        verify(notificationService).enviarConfirmacion("cliente1", "cliente@ticketya.com", "orden-1");
    }

    @Test
    void devolucionAprobadaNotificaAlCliente() {
        DomainEvent event = evento("evt-devolucion", "devolucion.aprobada", "orden-2", Map.of(
                "ordenId", "orden-2",
                "clienteId", "cliente1",
                "motivo", "Solicitud aprobada"));

        consumer.handleNotificacion(event, null);

        verify(notificationService).enviarDevolucionAprobada(
                eq("cliente1"), any(), eq("orden-2"), eq("Solicitud aprobada"));
    }

    @Test
    void eventoCanceladoAvisaATodosLosCompradores() {
        DomainEvent event = evento("evt-cancelado", "evento.cancelado", "ev-1", Map.of(
                "eventoId", "ev-1",
                "nombre", "Rock Fest"));

        consumer.handleNotificacion(event, null);

        verify(notificationService).enviarAvisoEvento(
                eq("ev-1"), contains("Evento cancelado: Rock Fest"), anyString());
    }

    @Test
    void eventoModificadoAvisaATodosLosCompradores() {
        DomainEvent event = evento("evt-modificado", "evento.modificado", "ev-1", Map.of(
                "eventoId", "ev-1",
                "nombre", "Rock Fest",
                "detalle", "Nuevo horario: 21:00"));

        consumer.handleNotificacion(event, null);

        verify(notificationService).enviarAvisoEvento(
                eq("ev-1"), contains("Evento modificado: Rock Fest"), eq("Nuevo horario: 21:00"));
    }

    @Test
    void eventoDuplicadoSeProcesaUnaSolaVez() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true, false);
        DomainEvent event = evento("evt-duplicado", "pago.aprobado", "orden-1", Map.of(
                "ordenId", "orden-1",
                "clienteId", "cliente1"));

        consumer.handleNotificacion(event, null);
        consumer.handleNotificacion(event, null);

        verify(notificationService, times(1))
                .enviarConfirmacion("cliente1", null, "orden-1");
    }

    @Test
    void falloDeCorreoLiberaDedupeYPermiteReintento() {
        doThrow(new MailSendException("MailHog caído"))
                .when(notificationService).enviarConfirmacion(any(), any(), anyString());
        DomainEvent event = evento("evt-falla", "pago.aprobado", "orden-1", Map.of(
                "ordenId", "orden-1",
                "clienteId", "cliente1"));

        assertThrows(MailSendException.class, () -> consumer.handleNotificacion(event, null));

        verify(redisTemplate).delete("seen:notificaciones.q:evt-falla");
        verify(notificationService, times(1))
                .enviarConfirmacion(any(), any(), anyString());
    }

    @Test
    void eventoDesconocidoNoDisparaNotificaciones() {
        DomainEvent event = evento("evt-otro", "otro.evento", "x", Map.of());

        consumer.handleNotificacion(event, null);

        verify(notificationService, never()).enviarConfirmacion(any(), any(), any());
        verify(notificationService, never()).enviarAvisoEvento(any(), any(), any());
        verify(notificationService, never()).enviarDevolucionAprobada(any(), any(), any(), any());
    }

    private DomainEvent evento(String eventId, String tipo, String aggregateId, Map<String, Object> payload) {
        return new DomainEvent(eventId, tipo, aggregateId, payload, "corr-test", Instant.now());
    }
}
