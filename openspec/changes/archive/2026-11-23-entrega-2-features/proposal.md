# Proposal: Funcionalidades pendientes y requisitos no funcionales (Entrega 2)

## Why

La entrega 1 dejó implementadas solo las features 1–3 (catálogo, reserva anti-doble venta, pago y emisión de QR), mientras las features 4–8 y los requisitos no funcionales de la consigna (consistencia con compensaciones, resiliencia, caché, trazabilidad, JWT, Docker y prueba de carga) están pendientes o solo parcialmente resueltos. Esta change completa el sistema con una implementación directa, fácil de explicar y defender oralmente.

## What Changes

- Completar devoluciones y cancelaciones: falta el rechazo administrativo (R7.2) y el reembolso en la cancelación masiva (R8.2).
- Agregar validación de entradas por QR en puerta, con prevención de reingreso (R9.1–R9.3).
- Agregar fila virtual con turnos y pases temporales para demanda extrema (R10.1–R10.3).
- Agregar notificaciones por correo y avisos ante cambios de evento (R11.1–R11.2).
- Agregar reportes de recaudación y ocupación por sector (R12.1–R12.2).
- Reutilizar la autenticación JWT y el RBAC ya implementados en `api-gateway` (WebFlux, usuarios precargados y roles `ROLE_*`).
- Resolver consistencia entre servicios con una Saga orquestada simple (`try/catch` + endpoints de compensación), sin patrón Outbox.
- Incorporar RabbitMQ con Spring AMQP para notificaciones y eventos asíncronos, sin DLQ.
- Incorporar Resilience4j (`@Retry`, `@TimeLimiter`, `@CircuitBreaker` con `fallbackMethod`) y timeouts HTTP.
- Incorporar caché de solo lectura del catálogo con Redis e invalidación ante cambios.
- Incorporar trazabilidad con `X-Correlation-ID`, MDC y logs estructurados centralizados en Loki/Grafana.
- Empaquetar cada microservicio en su propio contenedor Docker y ampliar `docker-compose.yml`.
- Generar plan de carga JMeter, ejecución no interactiva y reporte de resultados.

**BREAKING:** los endpoints pasan a exigir JWT vigente; el `clienteId` se toma del token y deja de aceptarse como parámetro de identidad no verificado. La reserva requiere un pase válido de la fila virtual cuando el evento esté en modo de alta demanda.

## Capabilities

### New Capabilities

- `ticket-refunds`: devoluciones, rechazo administrativo, cancelación masiva y reembolsos con liberación de localidades.
- `qr-access-control`: validación de QR en puerta, cambio a `USADA` y rechazo de reingresos o entradas inválidas.
- `virtual-queue`: turnos correlativos, pases temporales e invalidación por expiración.
- `notifications`: correos transaccionales y avisos de cambios o cancelación de eventos.
- `occupancy-metrics`: consulta de recaudación y ocupación por sector.
- `api-security`: reutilización del JWT/RBAC del gateway, autorización por roles y prevención de suplantación de cliente.
- `service-resilience`: saga con compensaciones, recuperación de estados pendientes y fallbacks ante fallas de dependencias.
- `catalog-cache`: respuestas de catálogo servidas desde caché de lectura, con invalidación y degradación segura.
- `async-messaging`: publicación y consumo idempotente de eventos de dominio con RabbitMQ y reintentos simples.
- `centralized-logging`: correlación de requests, logs estructurados y consulta centralizada con health checks.

### Modified Capabilities

- `ticket-sales`: el requisito R4.1 pasa a exigir pase válido de la fila virtual cuando el evento esté en modo de alta demanda.

## Impact

- La autenticación ya vive en `api-gateway`; no se agrega `auth-service` ni `db_auth`. MongoDB se mantiene en `eventos-service`, PostgreSQL en `compras-service` y `entradas-service`.
- Clientes HTTP con timeouts y anotaciones Resilience4j; filtro de correlación en gateway y servicios.
- Redis, RabbitMQ, Loki y Grafana como infraestructura en `docker-compose.yml`, además de un Dockerfile por servicio.
- Esquemas de la API documentados en `docs/openapi.yaml` (seguridad, validación de QR, fila, notificaciones y métricas).
- Decisiones arquitectónicas registradas como ADR cortos en `docs/adr/`.

## Out of Scope

- Cobros reales: la pasarela sigue siendo simulada.
- OAuth2/OpenID Connect externo y proveedores de identidad de terceros.
- Motor de reglas de notificaciones ni plantillas multilingües.
- Migración de los datos existentes a un nuevo esquema de bases.
