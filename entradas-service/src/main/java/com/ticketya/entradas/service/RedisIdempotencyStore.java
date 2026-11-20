package com.ticketya.entradas.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Component
public class RedisIdempotencyStore {

    private static final Duration RESULT_TTL = Duration.ofHours(24);
    private static final Duration LOCK_TTL = Duration.ofSeconds(60);
    private static final long WAIT_MILLIS = 5000;
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public RedisIdempotencyStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public <T> T execute(String operation, String idempotencyKey, Class<T> resultType, Supplier<T> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String resultKey = "idempotency:" + operation + ":" + hash(idempotencyKey);
        String lockKey = resultKey + ":lock";
        long deadline = System.nanoTime() + Duration.ofMillis(WAIT_MILLIS).toNanos();
        while (true) {
            T cached = get(resultKey, resultType);
            if (cached != null) {
                return cached;
            }

            String lockToken = UUID.randomUUID().toString();
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, lockToken, LOCK_TTL))) {
                try {
                    cached = get(resultKey, resultType);
                    if (cached != null) {
                        return cached;
                    }
                    T result = action.get();
                    put(resultKey, result);
                    return result;
                } finally {
                    redis.execute(RELEASE_LOCK, List.of(lockKey), lockToken);
                }
            }

            if (System.nanoTime() >= deadline) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "La operación con esta Idempotency-Key sigue en proceso; reintente");
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Se interrumpió la espera de la operación idempotente", error);
            }
        }
    }

    private <T> T get(String key, Class<T> resultType) {
        String serialized = redis.opsForValue().get(key);
        if (serialized == null) {
            return null;
        }
        try {
            return objectMapper.readValue(serialized, resultType);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("No se pudo deserializar el resultado idempotente", error);
        }
    }

    private void put(String key, Object result) {
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(result), RESULT_TTL);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("No se pudo serializar el resultado idempotente", error);
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no está disponible", error);
        }
    }
}
