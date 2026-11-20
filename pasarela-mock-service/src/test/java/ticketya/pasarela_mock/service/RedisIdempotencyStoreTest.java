package ticketya.pasarela_mock.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ticketya.pasarela_mock.dto.PagoResponse;

import java.time.Duration;
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
    void repiteElResultadoCacheadoSinEjecutarOtraVezLaOperacion() {
        PagoResponse resultado = new PagoResponse("txn-1", "APROBADO", "ok");
        when(values.get(anyString())).thenReturn(null, null,
                "{\"transaccionId\":\"txn-1\",\"estado\":\"APROBADO\",\"mensaje\":\"ok\"}");
        when(values.setIfAbsent(anyString(), anyString(), eq(Duration.ofSeconds(60)))).thenReturn(true);
        AtomicInteger ejecuciones = new AtomicInteger();

        PagoResponse primera = store.execute("pago", "clave-1", PagoResponse.class, () -> {
            ejecuciones.incrementAndGet();
            return resultado;
        });
        PagoResponse reintento = store.execute("pago", "clave-1", PagoResponse.class, () -> {
            ejecuciones.incrementAndGet();
            return new PagoResponse("txn-2", "APROBADO", "duplicado");
        });

        assertSame(resultado, primera);
        assertEquals("txn-1", reintento.getTransaccionId());
        assertEquals(1, ejecuciones.get());
        verify(values).set(startsWith("idempotency:pago:"), anyString(), eq(Duration.ofHours(24)));
    }
}
