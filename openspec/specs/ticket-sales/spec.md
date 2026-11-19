# ticket-sales Specification

## Purpose

Define el comportamiento observable del flujo central de TicketYa: administrar lugares y eventos, consultar el catálogo, reservar entradas por 10 minutos sin doble venta, cobrar la orden y emitir la entrada con su código QR.

## Requirements

### Requirement: Registro de lugares y coordenadas (R1.1)
When un organizador crea un lugar, the system SHALL registrar su ID, nombre, dirección y coordenadas geográficas.

#### Scenario: Lugar creado con datos completos
- **WHEN** un organizador envía un lugar con nombre, dirección y coordenadas geográficas
- **THEN** el sistema lo persiste con un ID único y lo devuelve en la respuesta

### Requirement: Exigencia de filas y asientos en sectores numerados (R1.2)
While se define un sector numerado, the system SHALL exigir la especificación de filas y asientos.

#### Scenario: Sector numerado incompleto
- **WHEN** un organizador define un sector numerado sin filas o sin asientos
- **THEN** el sistema rechaza la definición e indica qué datos faltan

### Requirement: Exigencia de capacidad en sectores generales (R1.3)
While se define un sector no numerado, the system SHALL exigir una capacidad máxima permitida.

#### Scenario: Sector general sin capacidad
- **WHEN** un organizador define un sector no numerado sin capacidad máxima
- **THEN** el sistema rechaza la definición e indica que la capacidad es obligatoria

### Requirement: Creación de eventos en borrador (R2.1)
When un organizador crea un evento, the system SHALL iniciarlo en estado `BORRADOR`.

#### Scenario: Evento nuevo
- **WHEN** un organizador crea un evento con sus datos básicos
- **THEN** el sistema lo persiste con estado `BORRADOR` y no lo publica en el catálogo

### Requirement: Publicación de eventos con precios por sector (R2.2)
When el organizador asigna precios a los sectores y confirma, the system SHALL cambiar el estado a `PUBLICADO`.

#### Scenario: Publicación tras asignar precios
- **WHEN** el organizador asigna precios a todos los sectores y confirma la publicación
- **THEN** el sistema cambia el evento a `PUBLICADO` y lo habilita para la venta

### Requirement: Bloqueo de venta de eventos en borrador (R2.3)
If un evento está en estado `BORRADOR`, then the system SHALL impedir la selección de entradas por los clientes.

#### Scenario: Intento de reserva en borrador
- **WHEN** un cliente intenta seleccionar entradas de un evento en estado `BORRADOR`
- **THEN** el sistema deniega la selección e informa que el evento no está publicado

### Requirement: Búsqueda multi-criterio de eventos (R3.1)
When un cliente consulta el catálogo, the system SHALL permitir filtros combinados por tipo de evento, organizador, artista, ciudad y rango de fechas.

#### Scenario: Filtros combinados
- **WHEN** un cliente consulta el catálogo con filtros de ciudad, tipo y rango de fechas
- **THEN** el sistema devuelve solo los eventos publicados que coinciden con todos los filtros

### Requirement: Reserva temporal con bloqueo (R4.1)
When un cliente selecciona asientos o lugares disponibles, the system SHALL generar una reserva temporal por 10 minutos y bloquear la selección a otros usuarios.

#### Scenario: Reserva exitosa
- **WHEN** un cliente selecciona un asiento disponible
- **THEN** el sistema crea una orden con vencimiento en 10 minutos y bloquea ese asiento para otros clientes

#### Scenario: Asiento ya bloqueado
- **WHEN** otro cliente intenta reservar el mismo asiento durante la reserva vigente
- **THEN** el sistema rechaza la reserva e informa que el asiento no está disponible

### Requirement: Liberación automática por vencimiento (R4.2)
If transcurren 10 minutos sin confirmación de pago, then the system SHALL liberar automáticamente los lugares.

#### Scenario: Reserva expirada
- **WHEN** transcurren 10 minutos desde la creación de la reserva sin confirmación de pago
- **THEN** el sistema marca la orden como expirada y libera los asientos bloqueados

### Requirement: Asignación única bajo concurrencia (R5.1)
When múltiples clientes intentan reservar el mismo asiento en paralelo, the system SHALL garantizar la asignación a una única solicitud mediante control de concurrencia e idempotencia.

#### Scenario: Solicitudes paralelas sobre el mismo asiento
- **WHEN** dos clientes envían simultáneamente una reserva para el mismo asiento
- **THEN** el sistema confirma una sola reserva y rechaza la otra sin duplicar el bloqueo

### Requirement: Emisión de entrada con QR tras pago aprobado (R5.2)
When la pasarela confirma el cobro dentro del tiempo de reserva, the system SHALL emitir la entrada con un código QR único.

#### Scenario: Pago aprobado
- **WHEN** la pasarela confirma el cobro de una orden vigente
- **THEN** el sistema emite una entrada con un código QR único y confirma la compra al cliente

### Requirement: Liberación inmediata tras pago rechazado (R5.3)
If la pasarela rechaza la transacción, then the system SHALL liberar inmediatamente los lugares reservados.

#### Scenario: Pago rechazado
- **WHEN** la pasarela rechaza el cobro de una orden con reserva vigente
- **THEN** el sistema marca la orden como rechazada y libera los asientos sin esperar el vencimiento
