# ADR 0006: Tolerancia a fallos con Resilience4j y timeouts

- **Fecha:** 2026-11-18
- **Estado:** Aceptado
- **Microservicio:** `compras-service`, `api-gateway`

## Contexto

Una pasarela o un microservicio lento puede bloquear la compra y dejar órdenes sin resolver. Los clientes actuales usan `RestClient` sin timeout.

## Decisión

Migrar a `WebClient` con `connectTimeout`/`responseTimeout` y aplicar `@Retry`, `@TimeLimiter` (métodos `CompletableFuture`) y `@CircuitBreaker(fallbackMethod=...)` de Resilience4j. Reutilizar `Idempotency-Key` en pago y emisión.

**Alternativa descartada:** reintentos manuales con `Thread.sleep` (difíciles de configurar y probar).

## Consecuencias

- Fallos temporales se absorben sin duplicar cargos ni reservas.
- Los fallbacks devuelven error controlado o inician la compensación de la saga.
