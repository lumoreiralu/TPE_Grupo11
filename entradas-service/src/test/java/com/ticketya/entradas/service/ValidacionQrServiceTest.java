package com.ticketya.entradas.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ticketya.entradas.client.EventosClient;
import com.ticketya.entradas.dto.ValidarQrRequest;
import com.ticketya.entradas.dto.ValidarQrResponse;
import com.ticketya.entradas.model.Entrada;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.repository.EntradaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
class ValidacionQrServiceTest {

    private static final String EVENTO_HOY = "evento-hoy";
    private static final String ROL = "ROLE_PERSONAL_ACCESO";

    @Autowired
    private EntradaService entradaService;

    @Autowired
    private EntradaRepository entradaRepository;

    @MockitoBean
    private EventosClient eventosClient;

    @BeforeEach
    void setUp() {
        entradaRepository.deleteAll();
        when(eventosClient.obtenerFechaEvento(anyString()))
                .thenReturn(CompletableFuture.completedFuture(LocalDateTime.now()));
    }

    @Test
    void primerIngresoAutorizaYRegistraFecha() {
        Entrada entrada = guardarEntrada("hash-primer-ingreso", EVENTO_HOY, EstadoEntrada.EMITIDA);

        ValidarQrResponse response = entradaService.validarQr(
                request("hash-primer-ingreso", EVENTO_HOY), "validador-1", ROL);

        assertEquals(EstadoEntrada.USADA, response.estado());
        assertNotNull(response.fechaIngreso());
        assertEquals("Acceso autorizado", response.mensaje());

        Entrada persistida = entradaRepository.findById(entrada.getId()).orElseThrow();
        assertEquals(EstadoEntrada.USADA, persistida.getEstado());
        assertNotNull(persistida.getFechaIngreso());
    }

    @Test
    void dobleEscaneoConcurrenteAutorizaSoloUnaEntrada() throws Exception {
        Entrada entrada = guardarEntrada("hash-concurrente", EVENTO_HOY, EstadoEntrada.EMITIDA);
        ValidarQrRequest request = request("hash-concurrente", EVENTO_HOY);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<Resultado>> futuros = new ArrayList<>();

        try {
            for (int i = 0; i < 2; i++) {
                futuros.add(executor.submit(() -> {
                    listos.countDown();
                    inicio.await();
                    try {
                        ValidarQrResponse response = entradaService.validarQr(
                                request, "validador-" + Thread.currentThread().getId(), ROL);
                        return new Resultado(response, null);
                    } catch (RuntimeException error) {
                        return new Resultado(null, error);
                    }
                }));
            }

            assertTrue(listos.await(5, TimeUnit.SECONDS));
            inicio.countDown();

            List<Resultado> resultados = new ArrayList<>();
            for (Future<Resultado> futuro : futuros) {
                resultados.add(futuro.get(10, TimeUnit.SECONDS));
            }

            assertEquals(1, resultados.stream().filter(resultado -> resultado.response() != null).count());
            assertEquals(1, resultados.stream().filter(resultado -> esConflicto(resultado.error())).count());
        } finally {
            inicio.countDown();
            executor.shutdownNow();
        }

        assertEquals(EstadoEntrada.USADA, entradaRepository.findById(entrada.getId()).orElseThrow().getEstado());
    }

    @Test
    void reingresoSeDeniegaYRegistraAlerta() {
        guardarEntrada("hash-reingreso", EVENTO_HOY, EstadoEntrada.USADA);
        Logger logger = (Logger) LoggerFactory.getLogger(EntradaService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        MDC.put("correlationId", "corr-reingreso-123");

        try {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> entradaService.validarQr(request("hash-reingreso", EVENTO_HOY), "validador-2", ROL));
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
            assertTrue(error.getReason().contains("reingreso"));
        } finally {
            logger.detachAppender(appender);
            MDC.clear();
        }

        assertTrue(appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .anyMatch(mensaje -> mensaje.contains("reingreso")
                        && mensaje.contains("corr-reingreso-123")
                        && mensaje.contains(ROL)));
    }

    @Test
    void qrInexistenteSeDeniegaCon404() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> entradaService.validarQr(request("hash-inexistente", EVENTO_HOY), "validador-3", ROL));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void qrDeOtroEventoSeDeniegaCon400() {
        guardarEntrada("hash-otro-evento", "evento-a", EstadoEntrada.EMITIDA);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> entradaService.validarQr(request("hash-otro-evento", "evento-b"), "validador-4", ROL));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("otro evento"));
    }

    @Test
    void qrCanceladoSeDeniegaCon400() {
        guardarEntrada("hash-cancelada", EVENTO_HOY, EstadoEntrada.CANCELADA);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> entradaService.validarQr(request("hash-cancelada", EVENTO_HOY), "validador-5", ROL));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("cancelada"));
    }

    @Test
    void eventoNoVigenteSeDeniegaCon400() {
        guardarEntrada("hash-evento-vencido", EVENTO_HOY, EstadoEntrada.EMITIDA);
        when(eventosClient.obtenerFechaEvento(EVENTO_HOY))
                .thenReturn(CompletableFuture.completedFuture(LocalDateTime.now().minusDays(1)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> entradaService.validarQr(request("hash-evento-vencido", EVENTO_HOY), "validador-6", ROL));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertTrue(error.getReason().contains("vigente"));
    }

    private Entrada guardarEntrada(String hash, String eventoId, EstadoEntrada estado) {
        Entrada entrada = new Entrada();
        entrada.setOrdenCompraId("orden-" + hash);
        entrada.setClienteId("cliente-1");
        entrada.setEventoId(eventoId);
        entrada.setSectorNombre("Platea");
        entrada.setFila("A");
        entrada.setAsiento(1);
        entrada.setCodigoQrHash(hash);
        entrada.setEstado(estado);
        if (estado == EstadoEntrada.USADA) {
            entrada.setFechaIngreso(LocalDateTime.now().minusMinutes(5));
        }
        return entradaRepository.saveAndFlush(entrada);
    }

    private ValidarQrRequest request(String hash, String eventoId) {
        return new ValidarQrRequest(hash, eventoId);
    }

    private boolean esConflicto(RuntimeException error) {
        return error instanceof ResponseStatusException responseStatusException
                && responseStatusException.getStatusCode().value() == HttpStatus.CONFLICT.value();
    }

    private record Resultado(ValidarQrResponse response, RuntimeException error) {
    }
}
