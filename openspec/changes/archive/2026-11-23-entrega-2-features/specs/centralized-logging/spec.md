# Spec Delta

## Purpose

Hace posible seguir una petición de punta a punta mediante un identificador de correlación propagado por gateway, servicios y eventos, con logs estructurados consultables en forma centralizada y estados de salud expuestos.

## ADDED Requirements

### Requirement: Propagación del identificador de correlación (LOG-1)
When ingresa una petición al gateway, the system SHALL generar o conservar un header `X-Correlation-ID` y propagarlo a todos los microservicios y eventos derivados de esa petición.

#### Scenario: Petición nueva
- **WHEN** un cliente realiza una petición sin identificador de correlación
- **THEN** el gateway genera uno, lo reenvía a los servicios y lo incluye en la respuesta

#### Scenario: Correlación entre servicios
- **WHEN** `compras-service` llama a `entradas-service` durante una compra
- **THEN** ambos servicios registran los mismos identificadores de correlación en sus bitácoras

### Requirement: Logs estructurados (LOG-2)
When un servicio registra un evento operativo, the system SHALL incluir el identificador de correlación, el nombre del servicio, el nivel, la marca de tiempo y un mensaje legible en formato estructurado.

#### Scenario: Error con contexto
- **WHEN** una compensación falla durante una compra
- **THEN** el sistema emite un log estructurado con el correlation ID, el servicio, el nivel de error y el motivo de la falla

### Requirement: Consulta centralizada y health checks (LOG-3)
When un operador consulta la bitácora, the system SHALL permitir filtrar por identificador de correlación y por servicio, y exponer el estado de salud de cada microservicio.

#### Scenario: Búsqueda por correlación
- **WHEN** un operador busca los logs de un `X-Correlation-ID`
- **THEN** el agregador devuelve los registros de todos los servicios intervinientes en esa petición

#### Scenario: Estado de un servicio
- **WHEN** se consulta el endpoint de salud de un microservicio
- **THEN** el sistema informa si está disponible para recibir tráfico
