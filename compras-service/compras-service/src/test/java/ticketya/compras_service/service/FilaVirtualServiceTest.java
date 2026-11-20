package ticketya.compras_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import ticketya.compras_service.client.EventoDTO;
import ticketya.compras_service.config.QueueConfig;
import ticketya.compras_service.dto.FilaJoinResponse;
import ticketya.compras_service.dto.FilaTurnoResponse;
import ticketya.compras_service.exception.NegocioException;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilaVirtualServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ObjectProvider<StringRedisTemplate> redisProvider;
    @Mock ValueOperations<String, String> valueOps;
    @Mock ZSetOperations<String, String> zsetOps;
    @Mock SetOperations<String, String> setOps;
    @Mock QueueConfig config;

    FilaVirtualService service;

    @BeforeEach
    void setUp() {
        when(redisProvider.getIfAvailable()).thenReturn(redis);
        lenient().when(config.paseTtl()).thenReturn(Duration.ofMinutes(10));
        lenient().when(config.umbralPorDefecto()).thenReturn(50);
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
        lenient().when(redis.opsForZSet()).thenReturn(zsetOps);
        lenient().when(redis.opsForSet()).thenReturn(setOps);
        service = new FilaVirtualService(redisProvider, config);
    }

    @Test
    void joinEnAltaDemandaAsignaTurnosCorrelativos() {
        EventoDTO evento = eventoAltaDemanda();
        when(valueOps.get("active:evt-1:cli-1")).thenReturn(null);
        when(zsetOps.score("queue:evt-1", "cli-1")).thenReturn(null);
        // después de promover, simulamos que no obtuvo pase y quedó en posición 1
        when(zsetOps.rank("queue:evt-1", "cli-1")).thenReturn(0L);

        FilaJoinResponse r1 = service.join("evt-1", "cli-1", evento);

        verify(zsetOps).add(eq("queue:evt-1"), eq("cli-1"), anyDouble());
        assertFalse(r1.turnoActivo());
        assertEquals(1, r1.posicion());

        // segundo cliente entra después y queda detrás
        when(valueOps.get("active:evt-1:cli-2")).thenReturn(null);
        when(zsetOps.score("queue:evt-1", "cli-2")).thenReturn(null);
        when(zsetOps.rank("queue:evt-1", "cli-2")).thenReturn(1L);

        FilaJoinResponse r2 = service.join("evt-1", "cli-2", evento);
        assertFalse(r2.turnoActivo());
        assertEquals(2, r2.posicion());
    }

    @Test
    void turnoDevuelvePosicionPorZrank() {
        when(valueOps.get("active:evt-1:cli-3")).thenReturn(null);
        when(zsetOps.rank("queue:evt-1", "cli-3")).thenReturn(4L);

        FilaTurnoResponse turno = service.turno("evt-1", "cli-3");

        assertFalse(turno.turnoActivo());
        assertEquals(5, turno.posicion());
        assertNull(turno.paseId());
    }

    @Test
    void joinOtorgaPaseCuandoEsPromovido() {
        EventoDTO evento = eventoAltaDemanda();
        java.util.concurrent.atomic.AtomicReference<String> paseActivo = new java.util.concurrent.atomic.AtomicReference<>();

        when(zsetOps.score("queue:evt-1", "cli-1")).thenReturn(null);
        when(zsetOps.zCard("queue:evt-1")).thenReturn(1L, 0L);
        when(zsetOps.range("queue:evt-1", 0, 49)).thenReturn(new LinkedHashSet<>(List.of("cli-1")));

        doAnswer(inv -> {
            String key = inv.getArgument(0);
            if ("active:evt-1:cli-1".equals(key)) return paseActivo.get();
            if (key != null && key.startsWith("pass:")) {
                String pid = key.substring("pass:".length());
                return pid.equals(paseActivo.get()) ? "evt-1|cli-1" : null;
            }
            return null;
        }).when(valueOps).get(anyString());
        doAnswer(inv -> {
            String key = inv.getArgument(0);
            String value = inv.getArgument(1);
            if ("active:evt-1:cli-1".equals(key)) {
                paseActivo.set(value);
            }
            return null;
        }).when(valueOps).set(anyString(), anyString(), any(Duration.class));
        when(redis.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(600L);

        FilaJoinResponse resp = service.join("evt-1", "cli-1", evento);

        assertTrue(resp.turnoActivo());
        assertNotNull(resp.paseId());
        assertEquals(0, resp.posicion());
        assertEquals(paseActivo.get(), resp.paseId());
        verify(valueOps, atLeastOnce()).set(startsWith("pass:"), eq("evt-1|cli-1"), any(Duration.class));
        verify(valueOps).set(eq("active:evt-1:cli-1"), anyString(), any(Duration.class));
    }

    @Test
    void paseExpiradoPermitePromoverAlSiguiente() {
        EventoDTO evento = eventoAltaDemanda();
        // cli-1 tenía pase pero expiró (clave pass inexistente)
        when(zsetOps.zCard("queue:evt-1")).thenReturn(2L);
        when(zsetOps.range("queue:evt-1", 0, 49)).thenReturn(new LinkedHashSet<>(List.of("cli-1", "cli-2")));
        when(valueOps.get("active:evt-1:cli-1")).thenReturn("pase-viejo");
        when(valueOps.get("pass:pase-viejo")).thenReturn(null); // expirado
        when(valueOps.get("active:evt-1:cli-2")).thenReturn(null);

        service.promoverEvento("evt-1", evento);

        // se crea pase nuevo para cli-1 (su pase viejo no es válido) y para cli-2
        verify(valueOps, atLeastOnce()).set(startsWith("pass:"), eq("evt-1|cli-1"), any(Duration.class));
        verify(valueOps, atLeastOnce()).set(startsWith("pass:"), eq("evt-1|cli-2"), any(Duration.class));
        verify(zsetOps).remove("queue:evt-1", "cli-1");
        verify(zsetOps).remove("queue:evt-1", "cli-2");
    }

    @Test
    void reservaSinPaseEnAltaDemandaSeDeniega() {
        EventoDTO evento = eventoAltaDemanda();

        NegocioException ex = assertThrows(NegocioException.class,
                () -> service.exigirPaseParaReserva(evento, "cli-1", null));

        assertEquals(409, ex.getStatus().value());
        assertTrue(ex.getMessage().contains("alta demanda"));
    }

    @Test
    void reservaConPaseVigenteContinua() {
        EventoDTO evento = eventoAltaDemanda();
        when(valueOps.get("pass:pase-ok")).thenReturn("evt-1|cli-1");

        assertDoesNotThrow(() -> service.exigirPaseParaReserva(evento, "cli-1", "pase-ok"));
    }

    @Test
    void eventoNormalNoExigePase() {
        EventoDTO evento = new EventoDTO("evt-2", "PUBLICADO", "NORMAL", null, List.of(), List.of());
        assertDoesNotThrow(() -> service.exigirPaseParaReserva(evento, "cli-1", null));
    }

    @Test
    void validarRechazaPaseInexistente() {
        when(valueOps.get("pass:nope")).thenReturn(null);
        assertFalse(service.validar("nope", "evt-1", "cli-1").valido());
    }

    private EventoDTO eventoAltaDemanda() {
        return new EventoDTO("evt-1", "PUBLICADO", "ALTA_DEMANDA", 50, List.of(), List.of());
    }
}
