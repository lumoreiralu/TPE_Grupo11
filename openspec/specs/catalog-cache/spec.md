# catalog-cache Specification

## Purpose
Mejora la latencia de las consultas de catálogo con una caché de solo lectura que se invalida ante cambios de eventos y degrada con seguridad cuando la caché no está disponible, sin intervenir en los bloqueos de reserva.

## Requirements

### Requirement: Consulta de catálogo desde caché (CACHE-1)
When un cliente consulta el catálogo de eventos publicados, the system SHALL intentar servir la respuesta desde una caché de lectura con expiración configurada antes de acceder a MongoDB.

#### Scenario: Acierto de caché
- **WHEN** dos clientes consultan el mismo catálogo dentro del tiempo de expiración
- **THEN** la segunda respuesta se sirve desde la caché sin una nueva consulta a MongoDB

#### Scenario: Expiración de caché
- **WHEN** la entrada de caché supera su tiempo de expiración
- **THEN** el sistema consulta MongoDB, devuelve los datos actualizados y renueva la caché

### Requirement: Invalidación ante cambios de evento (CACHE-2)
When se publica, modifica o cancela un evento, the system SHALL invalidar las entradas de caché afectadas para que la siguiente consulta muestre el estado vigente.

#### Scenario: Evento cancelado
- **WHEN** un evento publicado pasa a estado `CANCELADO`
- **THEN** el sistema invalida su entrada de caché y la siguiente consulta ya no lo muestra como disponible

### Requirement: Degradación segura sin caché (CACHE-3)
If la caché no está disponible, then the system SHALL resolver la consulta directamente desde la base de datos sin fallar la petición.

#### Scenario: Caché caída
- **WHEN** la caché rechaza las conexiones durante una consulta de catálogo
- **THEN** el sistema consulta MongoDB, responde normalmente y registra el evento en la bitácora
