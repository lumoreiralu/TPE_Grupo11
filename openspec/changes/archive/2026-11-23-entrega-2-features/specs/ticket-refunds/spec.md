# Spec Delta

## Purpose

Gestiona solicitudes de devolución, aprobaciones y rechazos, cancelaciones masivas de eventos y sus reembolsos, liberando las localidades para que vuelvan a estar disponibles.

## ADDED Requirements

### Requirement: Solicitud de devolución dentro del plazo (R6.1)
When un cliente solicita la devolución de una entrada con más de 72 horas de anticipación al inicio del evento, the system SHALL registrar la solicitud en estado `PENDIENTE`.

#### Scenario: Solicitud dentro del plazo
- **WHEN** un cliente solicita la devolución de una entrada `EMITIDA` con más de 72 horas de anticipación
- **THEN** el sistema crea la solicitud en estado `PENDIENTE` e informa el plazo de resolución

### Requirement: Rechazo por plazo expirado (R6.2)
If un cliente solicita la devolución con menos de 72 horas de anticipación al inicio del evento, then the system SHALL rechazar la solicitud informando que el plazo expiró.

#### Scenario: Solicitud fuera del plazo
- **WHEN** un cliente solicita la devolución con menos de 72 horas de anticipación
- **THEN** el sistema deniega la solicitud e informa que el plazo expiró

### Requirement: Rechazo por estado de la entrada (R6.3)
If la entrada no se encuentra en estado `EMITIDA`, then the system SHALL impedir la creación de la solicitud.

#### Scenario: Entrada ya usada o cancelada
- **WHEN** un cliente solicita la devolución de una entrada en estado `USADA` o `CANCELADA`
- **THEN** el sistema deniega la solicitud e informa el estado actual de la entrada

### Requirement: Aprobación con reembolso y liberación (R7.1)
When un administrador aprueba una solicitud de devolución, the system SHALL cambiar la entrada a `CANCELADA`, liberar inmediatamente el asiento o cupo correspondiente y ordenar el reintegro a la pasarela de pagos.

#### Scenario: Aprobación exitosa
- **WHEN** un administrador aprueba una solicitud `PENDIENTE`
- **THEN** el sistema marca la entrada como `CANCELADA`, libera el asiento, ordena el reintegro y registra la solicitud como `APROBADA`

### Requirement: Rechazo administrativo con motivo (R7.2)
When un administrador rechaza una solicitud de devolución, the system SHALL registrar el motivo de rechazo y mantener la entrada en estado `EMITIDA`.

#### Scenario: Rechazo con motivo
- **WHEN** un administrador rechaza una solicitud `PENDIENTE` indicando un motivo
- **THEN** el sistema registra el motivo, marca la solicitud como `RECHAZADA` y conserva la entrada en estado `EMITIDA`

### Requirement: Cancelación de evento (R8.1)
When se confirma la cancelación de un evento, the system SHALL cambiar su estado a `CANCELADO` e impedir cualquier nueva reserva.

#### Scenario: Cancelación publicada
- **WHEN** un organizador confirma la cancelación de un evento publicado
- **THEN** el sistema cambia el evento a `CANCELADO` y rechaza los intentos de reserva posteriores

### Requirement: Cancelación masiva con reembolso (R8.2)
When un evento pasa a estado `CANCELADO`, the system SHALL transicionar automáticamente todas sus entradas vendidas a `CANCELADA`, liberar los asientos o cupos y disparar la solicitud de reembolso a la pasarela de pagos.

#### Scenario: Reembolso de todas las entradas
- **WHEN** un evento pasa a estado `CANCELADO` con entradas vendidas
- **THEN** el sistema cancela todas sus entradas, libera los asientos y solicita el reembolso de cada orden afectada
