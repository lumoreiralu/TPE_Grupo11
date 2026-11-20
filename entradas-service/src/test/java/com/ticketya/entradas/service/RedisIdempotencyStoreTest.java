package com.ticketya.entradas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketya.entradas.dto.EmisionEntradasResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

class RedisIdempotencyStoreTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final RedisIdempotencyStore store = new RedisIdempotencyStore(redis);

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void devuelveLaEmisionSerializadaDeLaPrimeraSolicitudDurante24Horas() {
        EmisionEntradasResponse resultado = new EmisionEntradasResponse("orden-1", List.of());
        String serialized = "{\"ordenCompraId\":\"orden-1\",\"entradas\":[]}";
        when(values.get(anyString())).thenReturn(null, null, serialized);
        when(values.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(60)))).thenReturn(true);
        AtomicInteger ejecuciones = new AtomicInteger();

        EmisionEntradasResponse primera = store.execute("emision", "clave-1",
                EmisionEntradasResponse.class, () -> {
                    ejecuciones.incrementAndGet();
                    return resultado;
                });
        EmisionEntradasResponse reintento = store.execute("emision", "clave-1",
                EmisionEntradasResponse.class, () -> {
                    ejecuciones.incrementAndGet();
                    return new EmisionEntradasResponse("orden-2", List.of());
                });

        assertSame(resultado, primera);
        assertEquals("orden-1", reintento.ordenCompraId());
        assertEquals(1, ejecuciones.get());
        verify(values).set(startsWith("idempotency:emision:"), anyString(), eq(Duration.ofHours(24)));
    }
}
