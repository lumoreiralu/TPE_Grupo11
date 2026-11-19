# ADR 0010: Mensajería asíncrona con RabbitMQ simple, sin DLQ

- **Fecha:** 2026-11-18
- **Estado:** Aceptado
- **Microservicio:** `compras-service`, `entradas-service`, `eventos-service`

## Contexto

Correos, avisos e invalidación de caché no deben retrasar la respuesta de la compra, pero tampoco duplicarse.

## Decisión

Exchange `ticketya.topic`, una cola por consumidor, publicación `AFTER_COMMIT` con `@Async`, dedupe en Redis por `eventId` y reintento simple (3 intentos). Al agotar intentos se registra ERROR con el payload y se hace `ack`; no se crea DLQ.

**Alternativa descartada:** DLQ + reproceso automático (más infraestructura que la pedida para esta entrega).

## Consecuencias

- Notificaciones desacopladas y consumidores idempotentes.
- Un mensaje fallido queda solo en Loki y requiere intervención manual.
