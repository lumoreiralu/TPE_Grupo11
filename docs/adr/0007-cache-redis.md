# ADR 0007: Redis para caché de catálogo, fila virtual y dedupe

- **Fecha:** 2026-11-18
- **Estado:** Aceptado
- **Microservicio:** `eventos-service`, `compras-service`

## Contexto

El catálogo se consulta mucho y la fila virtual necesita turnos con expiración corta; los consumidores de eventos deben identificar duplicados.

## Decisión

Usar un único Redis: `@Cacheable` con TTL de 60 s e invalidación ante cambios de evento; sorted set `queue:{eventoId}` para turnos y clave `pass:{paseId}` con TTL de 600 s; claves `seen:{cola}:{eventId}` para dedupe. Ante caída de Redis, la caché consulta MongoDB y la fila degrada a modo normal.

**Alternativa descartada:** tablas PostgreSQL para la fila (más limpieza y peor latencia).

## Consecuencias

- Menor latencia y fila sencilla de demostrar.
- Redis pasa a ser dependencia compartida; se compensa con degradación segura.
