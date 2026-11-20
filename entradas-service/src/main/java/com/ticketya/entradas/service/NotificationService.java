package com.ticketya.entradas.service;

import com.ticketya.entradas.model.Entrada;
import com.ticketya.entradas.repository.EntradaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Notificaciones transaccionales por correo (R11).
 *
 * El envío se hace dentro del consumidor de RabbitMQ, por lo que un fallo de
 * MailHog/SMTP nunca bloquea la compra: solo se reintenta el correo y, si se
 * agotan los intentos, queda registrado en la bitácora para intervención manual.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final EntradaRepository entradaRepository;
    private final String remitente;
    private final String dominioEmail;
    private final int reintentos;
    private final long esperaEntreReintentosMs;

    public NotificationService(JavaMailSender mailSender,
                               EntradaRepository entradaRepository,
                               @Value("${notificaciones.remitente:no-reply@ticketya.com}") String remitente,
                               @Value("${notificaciones.dominio-email:ticketya.com}") String dominioEmail,
                               @Value("${notificaciones.reintentos:3}") int reintentos,
                               @Value("${notificaciones.reintento-espera-ms:1000}") long esperaEntreReintentosMs) {
        this.mailSender = mailSender;
        this.entradaRepository = entradaRepository;
        this.remitente = remitente;
        this.dominioEmail = dominioEmail;
        this.reintentos = Math.max(1, reintentos);
        this.esperaEntreReintentosMs = Math.max(0, esperaEntreReintentosMs);
    }

    /**
     * Confirma la compra y detalla las entradas emitidas para la orden.
     */
    public void enviarConfirmacion(String clienteId, String emailDestino, String ordenId) {
        List<Entrada> entradas = entradaRepository.findByOrdenCompraId(ordenId);
        String destinatario = resolverDestinatario(clienteId, emailDestino, entradas);
        if (destinatario == null) {
            log.warn("Confirmación de compra omitida: no se pudo resolver el correo de la orden {}", ordenId);
            return;
        }

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Hola").append(nombreVisible(clienteId, entradas)).append(",\n\n");
        cuerpo.append("Tu pago fue aprobado y tus entradas fueron emitidas.\n\n");
        cuerpo.append("Orden: ").append(ordenId).append('\n');

        if (entradas.isEmpty()) {
            cuerpo.append("Evento: (en emisión)\n");
            cuerpo.append("Las entradas están siendo emitidas; podés consultarlas en la aplicación.\n");
        } else {
            cuerpo.append("Evento: ").append(entradas.get(0).getEventoId()).append('\n');
            cuerpo.append("Entradas:\n");
            for (Entrada entrada : entradas) {
                cuerpo.append(" - Sector ").append(entrada.getSectorNombre())
                        .append(", fila ").append(entrada.getFila())
                        .append(", asiento ").append(entrada.getAsiento())
                        .append(" | QR: ").append(entrada.getCodigoQrHash())
                        .append('\n');
            }
        }

        cuerpo.append("\nGracias por comprar en TicketYa.\n");
        enviar(destinatario, "TicketYa - Confirmación de compra " + ordenId, cuerpo.toString());
    }

    /**
     * Avisa a todos los compradores con entradas de un evento que fue cancelado
     * o que cambió de horario, lugar o programación.
     */
    public void enviarAvisoEvento(String eventoId, String asunto, String detalle) {
        List<Entrada> entradas = entradaRepository.findByEventoId(eventoId);
        Map<String, Entrada> compradores = new LinkedHashMap<>();
        for (Entrada entrada : entradas) {
            if (tieneTexto(entrada.getClienteId())) {
                compradores.putIfAbsent(entrada.getClienteId(), entrada);
            }
        }

        if (compradores.isEmpty()) {
            log.warn("Aviso del evento {} omitido: no hay compradores con entradas registradas", eventoId);
            return;
        }

        for (Map.Entry<String, Entrada> comprador : compradores.entrySet()) {
            String clienteId = comprador.getKey();
            String destinatario = resolverDestinatario(clienteId, null, List.of(comprador.getValue()));
            if (destinatario == null) {
                continue;
            }

            StringBuilder cuerpo = new StringBuilder();
            cuerpo.append("Hola ").append(clienteId).append(",\n\n");
            cuerpo.append(asunto).append("\n\n");
            cuerpo.append(tieneTexto(detalle) ? detalle : "El evento cambió de programación.").append("\n\n");
            cuerpo.append("Evento: ").append(eventoId).append('\n');
            cuerpo.append("Si ya solicitaste tu devolución, el reintegro se procesará por el medio de pago original.\n\n");
            cuerpo.append("Gracias por comprar en TicketYa.\n");
            enviar(destinatario, asunto, cuerpo.toString());
        }

        log.info("Aviso del evento {} enviado a {} compradores", eventoId, compradores.size());
    }

    /**
     * Confirma al cliente que su solicitud de devolución fue aprobada.
     */
    public void enviarDevolucionAprobada(String clienteId, String emailDestino, String ordenId, String motivo) {
        List<Entrada> entradas = ordenId != null
                ? entradaRepository.findByOrdenCompraId(ordenId)
                : List.of();
        String destinatario = resolverDestinatario(clienteId, emailDestino, entradas);
        if (destinatario == null) {
            log.warn("Aviso de devolución omitido: no se pudo resolver el correo de la orden {}", ordenId);
            return;
        }

        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Hola,\n\n");
        cuerpo.append("Tu solicitud de devolución fue aprobada.\n\n");
        if (tieneTexto(ordenId)) {
            cuerpo.append("Orden: ").append(ordenId).append('\n');
        }
        if (tieneTexto(motivo)) {
            cuerpo.append("Detalle: ").append(motivo).append('\n');
        }
        cuerpo.append("El importe será reintegrado por el medio de pago original.\n\n");
        cuerpo.append("Gracias por comprar en TicketYa.\n");
        enviar(destinatario, "TicketYa - Devolución aprobada", cuerpo.toString());
    }

    private void enviar(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        MailException ultimoError = null;
        for (int intento = 1; intento <= reintentos; intento++) {
            try {
                mailSender.send(mensaje);
                log.info("Correo enviado a {} (asunto={}, intento={})", destinatario, asunto, intento);
                return;
            } catch (MailException error) {
                ultimoError = error;
                log.warn("Fallo temporal al enviar correo a {} (intento {}/{}): {}",
                        destinatario, intento, reintentos, error.getMessage());
                if (intento < reintentos) {
                    esperarEntreReintentos();
                }
            }
        }

        log.error("No se pudo enviar el correo a {} tras {} intentos (asunto={}): {}",
                destinatario, reintentos, asunto,
                ultimoError != null ? ultimoError.getMessage() : "sin detalle");
        throw ultimoError != null ? ultimoError : new IllegalStateException("No se pudo enviar el correo");
    }

    private void esperarEntreReintentos() {
        if (esperaEntreReintentosMs <= 0) {
            return;
        }
        try {
            Thread.sleep(esperaEntreReintentosMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envío de correo interrumpido", interrupted);
        }
    }

    private String resolverDestinatario(String clienteId, String emailDestino, List<Entrada> entradas) {
        if (tieneTexto(emailDestino) && emailDestino.contains("@")) {
            return emailDestino.trim();
        }
        if (tieneTexto(clienteId) && clienteId.contains("@")) {
            return clienteId.trim();
        }
        if (tieneTexto(clienteId)) {
            return clienteId.trim() + "@" + dominioEmail;
        }
        if (entradas != null) {
            for (Entrada entrada : entradas) {
                if (tieneTexto(entrada.getClienteId())) {
                    return entrada.getClienteId().trim() + "@" + dominioEmail;
                }
            }
        }
        return null;
    }

    private String nombreVisible(String clienteId, List<Entrada> entradas) {
        if (tieneTexto(clienteId)) {
            return " " + clienteId;
        }
        if (!entradas.isEmpty() && tieneTexto(entradas.get(0).getClienteId())) {
            return " " + entradas.get(0).getClienteId();
        }
        return "";
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
