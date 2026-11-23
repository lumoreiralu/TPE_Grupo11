# Spec Delta

## Purpose

Desacopla notificaciones, avisos y reportes del flujo síncrono del cliente mediante eventos de dominio publicados en RabbitMQ, con consumo idempotente y reintentos simples sin colas de死信 complejas.

## ADDED Requirements

### Requirement: Publicación de eventos de dominio (ASYNC-1)
When ocurre un pago aprobado, una emisión de entrada, una devolución aprobada o una cancelación de evento, the system SHALL publicar un evento asíncrono sin bloquear la respuesta al cliente.

#### Scenario: Pago aprobado
- **WHEN** se confirma el pago de una orden
- **THEN** el sistema responde al cliente y publica el evento de pago aprobado para los consumidores de notificaciones y métricas

### Requirement: Consumo idempotente (ASYNC-2)
When un consumidor recibe un evento duplicado o repetido tras un reintento, the system SHALL procesarlo una sola vez usando su identificador de evento.

#### Scenario: Evento repetido
- **WHEN** el consumidor de notificaciones recibe dos veces el mismo identificador de evento
- **THEN** el sistema envía un solo correo y descarta el duplicado

### Requirement: Reintento y registro de fallos (ASYNC-3)
If el consumo de un evento falla, then the system SHALL reintentarlo con espera progresiva y registrar el fallo en la bitácora al agotar los intentos, sin descartar silenciosamente el evento.

#### Scenario: Consumidor temporalmente caído
- **WHEN** el consumidor no puede procesar un evento por una falla temporal
- **THEN** el sistema reintenta el procesamiento y, si persiste el error, deja constancia del evento fallido para intervención manual
