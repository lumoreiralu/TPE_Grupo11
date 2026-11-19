# ADR 0005: Consistencia con Saga orquestada y compensaciones (sin Outbox)

- **Fecha:** 2026-11-18
- **Estado:** Aceptado
- **Microservicio:** `compras-service`

## Contexto

La compra cruza `compras-service`, `pasarela-mock-service` y `entradas-service`. Si falla la emisión después del pago, deben revertirse cargo, asientos y entradas sin transacción distribuida.

## Decisión

`CompraSagaService` orquesta reserva → pago → emisión en un `try`; en el `catch` ejecuta reembolso, liberación de asientos, borrado de entradas y marca la orden `COMPENSADA`. Un job periódico reconcilia órdenes `PROCESANDO_PAGO`. No se implementa Transactional Outbox.

**Alternativa descartada:** Outbox + worker (más garantías, pero demasiada complejidad para la defensa).

## Consecuencias

- Consistencia de dinero, entradas y cupos explicable con endpoints explícitos.
- Riesgo asumido: si RabbitMQ cae justo después del commit, la notificación se pierde; se mitiga con logs, reconciliación y compensación manual.
