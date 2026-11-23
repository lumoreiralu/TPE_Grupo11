# Spec Delta

## Purpose

Valida las entradas presentadas en el ingreso al recinto mediante su código QR, autorizando solo el primer ingreso de tickets vigentes y detectando reingresos o entradas inválidas.

## ADDED Requirements

### Requirement: Check-in con QR válido (R9.1)
When se escanea un código QR en estado `EMITIDA` que corresponde al evento del día, the system SHALL cambiar su estado a `USADA` y autorizar el acceso.

#### Scenario: Primer ingreso
- **WHEN** se invoca `POST /api/entradas/validar-qr` con el QR de una entrada `EMITIDA` del evento del día
- **THEN** el sistema cambia la entrada a `USADA` y autoriza el acceso

#### Scenario: Doble escaneo simultáneo
- **WHEN** dos puertas escanean simultáneamente el mismo QR válido
- **THEN** el sistema autoriza una sola entrada y rechaza la otra al encontrar la entrada ya `USADA`

### Requirement: Rechazo de reingreso (R9.2)
If se escanea un código QR cuya entrada ya figura en estado `USADA`, then the system SHALL rechazar el acceso alertando sobre intento de reingreso fraudulento.

#### Scenario: Reingreso intentado
- **WHEN** se escanea un QR cuya entrada ya está en estado `USADA`
- **THEN** el sistema deniega el acceso y registra la alerta de reingreso

### Requirement: Rechazo de QR inválido (R9.3)
If el código QR no existe, pertenece a otro evento o está en estado `CANCELADA`, then the system SHALL denegar el acceso.

#### Scenario: QR inexistente
- **WHEN** se escanea un código QR que no corresponde a ninguna entrada
- **THEN** el sistema deniega el acceso e informa que el QR no es válido

#### Scenario: QR de otro evento
- **WHEN** se escanea una entrada `EMITIDA` de un evento distinto al del recinto
- **THEN** el sistema deniega el acceso e informa la discrepancia de evento

#### Scenario: QR cancelado
- **WHEN** se escanea una entrada en estado `CANCELADA`
- **THEN** el sistema deniega el acceso e informa que la entrada fue cancelada
