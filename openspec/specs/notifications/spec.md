# notifications Specification

## Purpose
Envía comunicaciones por correo sobre compras y cambios de programación, desacoplando el envío del flujo del cliente mediante eventos asíncronos y reintentos simples.

## Requirements

### Requirement: Confirmación de compra por correo (R11.1)
When se completa exitosamente el pago de una orden, the system SHALL enviar un correo al cliente con la confirmación y el detalle de las entradas emitidas.

#### Scenario: Pago completado
- **WHEN** se confirma el pago y se emiten las entradas de una orden
- **THEN** el sistema publica la confirmación y envía un correo con el detalle de las entradas al cliente

#### Scenario: Fallo temporal del correo
- **WHEN** el servicio de correo rechaza temporalmente el envío
- **THEN** el sistema reintenta el envío y registra el resultado en la bitácora sin afectar la compra

### Requirement: Aviso por cambio o cancelación de evento (R11.2)
When un evento cambia de horario, lugar o es cancelado, the system SHALL enviar una notificación a todos los clientes con entradas adquiridas para ese evento.

#### Scenario: Evento cancelado
- **WHEN** un evento con entradas vendidas pasa a estado `CANCELADO`
- **THEN** el sistema publica el aviso y envía una notificación a cada cliente con entradas de ese evento
