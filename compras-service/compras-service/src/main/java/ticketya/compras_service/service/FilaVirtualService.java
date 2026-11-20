package ticketya.compras_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ticketya.compras_service.client.EventoDTO;
import ticketya.compras_service.config.QueueConfig;
import ticketya.compras_service.dto.FilaEstadoResponse;
import ticketya.compras_service.dto.FilaJoinResponse;
import ticketya.compras_service.dto.FilaTurnoResponse;
import ticketya.compras_service.dto.FilaValidarResponse;
import ticketya.compras_service.exception.NegocioException;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FilaVirtualService {

    private static final Logger log = LoggerFactory.getLogger(FilaVirtualService.class);
    private static final String COLAS_ACTIVAS = "queues:active";

    private final StringRedisTemplate redis;
    private final QueueConfig config;

    public FilaVirtualService(ObjectProvider<StringRedisTemplate> redisProvider, QueueConfig config) {
        this.redis = redisProvider.getIfAvailable();
        this.config = config;
    }

    private StringRedisTemplate redis() {
        if (redis == null) {
            throw new NegocioException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La fila virtual no está disponible en este momento");
        }
        return redis;
    }

    public boolean filaRequerida(EventoDTO evento) {
        return evento != null && "ALTA_DEMANDA".equalsIgnoreCase(evento.modoFila());
    }

    public FilaJoinResponse join(String eventoId, String clienteId, EventoDTO evento) {
        validarIds(eventoId, clienteId);

        String paseActivo = paseActivo(eventoId, clienteId);
        if (paseActivo != null && paseValido(paseActivo, eventoId, clienteId)) {
            Long ttl = ttlSegundos(passKey(paseActivo));
            return new FilaJoinResponse(eventoId, clienteId, 0, true, paseActivo, ttl,
                    "Ya tenés un pase activo para este evento.");
        }

        if (!filaRequerida(evento)) {
            String paseId = crearPase(eventoId, clienteId, config.paseTtl());
            Long ttl = ttlSegundos(passKey(paseId));
            return new FilaJoinResponse(eventoId, clienteId, 0, true, paseId, ttl,
                    "El evento no está en alta demanda, podés continuar con la compra.");
        }

        String queueKey = queueKey(eventoId);
        Double score = redis().opsForZSet().score(queueKey, clienteId);
        if (score == null) {
            redis().opsForZSet().add(queueKey, clienteId, System.currentTimeMillis());
            redis().opsForSet().add(COLAS_ACTIVAS, eventoId);
        }
        promoverEvento(eventoId, evento);

        return joinResponse(turno(eventoId, clienteId));
    }

    public FilaTurnoResponse turno(String eventoId, String clienteId) {
        validarIds(eventoId, clienteId);

        String paseActivo = paseActivo(eventoId, clienteId);
        if (paseActivo != null && paseValido(paseActivo, eventoId, clienteId)) {
            Long ttl = ttlSegundos(passKey(paseActivo));
            return new FilaTurnoResponse(eventoId, clienteId, 0, true, paseActivo, ttl);
        }

        Long rank = redis().opsForZSet().rank(queueKey(eventoId), clienteId);
        if (rank == null) {
            return new FilaTurnoResponse(eventoId, clienteId, 0, false, null, null);
        }
        return new FilaTurnoResponse(eventoId, clienteId, rank + 1, false, null, null);
    }

    public FilaValidarResponse validar(String paseId, String eventoId, String clienteId) {
        if (paseValido(paseId, eventoId, clienteId)) {
            return new FilaValidarResponse(true, "Pase válido");
        }
        return new FilaValidarResponse(false, "El pase no es válido o ya expiró");
    }

    public void exigirPaseParaReserva(EventoDTO evento, String clienteId, String paseId) {
        if (!filaRequerida(evento)) {
            return;
        }
        if (paseId == null || paseId.isBlank()) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "El evento está en alta demanda. Debés esperar tu turno en la fila virtual para obtener un pase.");
        }
        if (!paseValido(paseId, evento.id(), clienteId)) {
            throw new NegocioException(HttpStatus.CONFLICT,
                    "Tu pase de fila virtual no es válido o expiró. Esperá tu turno para obtener uno nuevo.");
        }
    }

    public void consumirPase(String paseId, String eventoId, String clienteId) {
        if (paseId == null || paseId.isBlank()) return;
        if (paseValido(paseId, eventoId, clienteId)) {
            redis().delete(passKey(paseId));
            redis().delete(activeKey(eventoId, clienteId));
        }
    }

    public FilaEstadoResponse estado(String eventoId, EventoDTO evento) {
        long enCola = size(queueKey(eventoId));
        String modo = evento != null && evento.modoFila() != null ? evento.modoFila() : "NORMAL";
        int umbral = umbralEfectivo(evento);
        return new FilaEstadoResponse(eventoId, modo, umbral, enCola, 0);
    }

    public void promoverColas() {
        Set<String> eventos = redis().opsForSet().members(COLAS_ACTIVAS);
        if (eventos == null || eventos.isEmpty()) return;
        for (String eventoId : eventos) {
            try {
                promoverEvento(eventoId, null);
            } catch (Exception e) {
                log.warn("Error promoviendo fila para evento {}: {}", eventoId, e.getMessage());
            }
        }
    }

    public void promoverEvento(String eventoId, EventoDTO evento) {
        String queueKey = queueKey(eventoId);
        Long cantidad = redis().opsForZSet().zCard(queueKey);
        if (cantidad == null || cantidad == 0) {
            redis().opsForSet().remove(COLAS_ACTIVAS, eventoId);
            return;
        }
        int cupos = Math.max(1, Math.min(umbralEfectivo(evento), 50));
        Set<String> primeros = redis().opsForZSet().range(queueKey, 0, cupos - 1);
        if (primeros == null || primeros.isEmpty()) return;

        for (String clienteId : primeros) {
            String actual = paseActivo(eventoId, clienteId);
            if (actual != null && paseValido(actual, eventoId, clienteId)) {
                redis().opsForZSet().remove(queueKey, clienteId);
                continue;
            }
            String paseId = crearPase(eventoId, clienteId, config.paseTtl());
            redis().opsForZSet().remove(queueKey, clienteId);
            log.debug("Pase {} otorgado a cliente {} para evento {}", paseId, clienteId, eventoId);
        }

        Long restantes = redis().opsForZSet().zCard(queueKey);
        if (restantes == null || restantes == 0) {
            redis().opsForSet().remove(COLAS_ACTIVAS, eventoId);
        }
    }

    private String crearPase(String eventoId, String clienteId, Duration ttl) {
        String paseId = UUID.randomUUID().toString();
        String valor = eventoId + "|" + clienteId;
        redis().opsForValue().set(passKey(paseId), valor, ttl);
        redis().opsForValue().set(activeKey(eventoId, clienteId), paseId, ttl);
        return paseId;
    }

    private boolean paseValido(String paseId, String eventoId, String clienteId) {
        if (paseId == null || paseId.isBlank() || eventoId == null || clienteId == null) return false;
        String valor = redis().opsForValue().get(passKey(paseId));
        if (valor == null) return false;
        return (eventoId + "|" + clienteId).equals(valor);
    }

    private String paseActivo(String eventoId, String clienteId) {
        if (eventoId == null || clienteId == null) return null;
        return redis().opsForValue().get(activeKey(eventoId, clienteId));
    }

    private Long ttlSegundos(String key) {
        Long ttl = redis().getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl < 0) return null;
        return ttl;
    }

    private long size(String key) {
        Long v = redis().opsForZSet().zCard(key);
        return v == null ? 0 : v;
    }

    private int umbralEfectivo(EventoDTO evento) {
        if (evento != null && evento.umbralConcurrencia() != null && evento.umbralConcurrencia() > 0) {
            return evento.umbralConcurrencia();
        }
        return Math.max(1, config.umbralPorDefecto());
    }

    private void validarIds(String eventoId, String clienteId) {
        if (eventoId == null || eventoId.isBlank() || clienteId == null || clienteId.isBlank()) {
            throw new NegocioException(HttpStatus.BAD_REQUEST, "eventoId y clienteId son obligatorios");
        }
    }

    private FilaJoinResponse joinResponse(FilaTurnoResponse turno) {
        String mensaje = turno.turnoActivo()
                ? "Tu turno está activo. Ya podés seleccionar entradas."
                : (turno.posicion() > 0
                    ? "Estás en la fila. Posición actual: " + turno.posicion()
                    : "No estás en la fila. Unite para obtener un turno.");
        return new FilaJoinResponse(turno.eventoId(), turno.clienteId(), turno.posicion(),
                turno.turnoActivo(), turno.paseId(), turno.segundosRestantesPase(), mensaje);
    }

    private String queueKey(String eventoId) { return "queue:" + eventoId; }
    private String passKey(String paseId) { return "pass:" + paseId; }
    private String activeKey(String eventoId, String clienteId) { return "active:" + eventoId + ":" + clienteId; }
}
