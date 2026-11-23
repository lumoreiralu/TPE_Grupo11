# Spec Delta

## MODIFIED Requirements

### Requirement: Reserva temporal con bloqueo (R4.1)
When un cliente selecciona asientos o lugares disponibles, the system SHALL generar una reserva temporal por 10 minutos y bloquear la selección a otros usuarios.

#### Scenario: Reserva exitosa
- **WHEN** un cliente selecciona un asiento disponible
- **THEN** el sistema crea una orden con vencimiento en 10 minutos y bloquea ese asiento para otros clientes

#### Scenario: Asiento ya bloqueado
- **WHEN** otro cliente intenta reservar el mismo asiento durante la reserva vigente
- **THEN** el sistema rechaza la reserva e informa que el asiento no está disponible

#### Scenario: Reserva sin pase de fila virtual
- **WHEN** un evento en modo de alta demanda recibe una reserva de un cliente sin pase vigente
- **THEN** el sistema deniega la reserva e indica que debe esperar su turno en la fila virtual
