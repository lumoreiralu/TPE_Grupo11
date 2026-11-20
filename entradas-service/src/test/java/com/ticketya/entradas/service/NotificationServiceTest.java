package com.ticketya.entradas.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ticketya.entradas.model.Entrada;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.repository.EntradaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EntradaRepository entradaRepository;

    @MockitoBean
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        entradaRepository.deleteAll();
    }

    @Test
    void confirmacionIncluyeDetalleDeEntradasEmitidas() {
        guardarEntrada("cliente1", "ev-1", "orden-1", "Platea", "A", 1, "qr-1");
        guardarEntrada("cliente1", "ev-1", "orden-1", "Platea", "A", 2, "qr-2");

        notificationService.enviarConfirmacion("cliente1", null, "orden-1");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mensaje = captor.getValue();

        assertEquals("cliente1@ticketya.com", mensaje.getTo()[0]);
        assertTrue(mensaje.getSubject().contains("Confirmación de compra"));
        assertTrue(mensaje.getText().contains("Orden: orden-1"));
        assertTrue(mensaje.getText().contains("Sector Platea"));
        assertTrue(mensaje.getText().contains("asiento 1"));
        assertTrue(mensaje.getText().contains("asiento 2"));
        assertTrue(mensaje.getText().contains("QR: qr-1"));
        assertTrue(mensaje.getText().contains("QR: qr-2"));
    }

    @Test
    void avisoDeEventoNotificaUnaVezACadaComprador() {
        guardarEntrada("cliente-a", "ev-1", "orden-a1", "Platea", "A", 1, "qr-a1");
        guardarEntrada("cliente-a", "ev-1", "orden-a2", "Platea", "A", 2, "qr-a2");
        guardarEntrada("cliente-b", "ev-1", "orden-b1", "Campo", "B", 3, "qr-b1");
        guardarEntrada("cliente-c", "ev-2", "orden-c1", "Campo", "C", 4, "qr-c1");

        notificationService.enviarAvisoEvento("ev-1",
                "TicketYa - Evento cancelado: Rock Fest",
                "El evento fue cancelado y tus entradas serán reintegradas.");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(captor.capture());
        List<String> destinatarios = captor.getAllValues().stream()
                .map(mensaje -> mensaje.getTo()[0])
                .sorted()
                .toList();

        assertEquals(List.of("cliente-a@ticketya.com", "cliente-b@ticketya.com"), destinatarios);
        assertTrue(captor.getAllValues().stream()
                .allMatch(mensaje -> mensaje.getText().contains("Evento: ev-1")));
    }

    @Test
    void falloTemporalDelCorreoSeReintenta() {
        guardarEntrada("cliente1", "ev-1", "orden-1", "Platea", "A", 1, "qr-1");
        doThrow(new MailSendException("MailHog no disponible"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        notificationService.enviarConfirmacion("cliente1", null, "orden-1");

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void falloPermanenteAgotaReintentosYRegistraEnBitacora() {
        guardarEntrada("cliente1", "ev-1", "orden-1", "Platea", "A", 1, "qr-1");
        doThrow(new MailSendException("MailHog caído"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertThrows(MailException.class,
                    () -> notificationService.enviarConfirmacion("cliente1", null, "orden-1"));
        } finally {
            logger.detachAppender(appender);
        }

        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
        assertTrue(appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(mensaje -> mensaje.contains("No se pudo enviar el correo")
                        && mensaje.contains("cliente1@ticketya.com")));
    }

    private Entrada guardarEntrada(String clienteId, String eventoId, String ordenId,
                                   String sector, String fila, int asiento, String qr) {
        Entrada entrada = new Entrada();
        entrada.setClienteId(clienteId);
        entrada.setEventoId(eventoId);
        entrada.setOrdenCompraId(ordenId);
        entrada.setSectorNombre(sector);
        entrada.setFila(fila);
        entrada.setAsiento(asiento);
        entrada.setCodigoQrHash(qr);
        entrada.setEstado(EstadoEntrada.EMITIDA);
        return entradaRepository.saveAndFlush(entrada);
    }
}
