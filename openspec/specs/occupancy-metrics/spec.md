# occupancy-metrics Specification

## Purpose
Expone para los organizadores la recaudación y la ocupación de sus eventos, derivadas de las órdenes y entradas existentes sin duplicar la propiedad de esos datos entre servicios.

## Requirements

### Requirement: Recaudación acumulada (R12.1)
When un organizador consulta el panel de métricas de su evento, the system SHALL calcular y mostrar la recaudación total acumulada.

#### Scenario: Consulta de recaudación
- **WHEN** un organizador consulta las métricas de un evento con ventas confirmadas
- **THEN** el sistema devuelve la suma de los montos de las órdenes pagadas de ese evento

### Requirement: Ocupación por sector (R12.2)
When se solicitan los reportes de ocupación, the system SHALL desglosar el porcentaje y cantidad de entradas vendidas, disponibles y devueltas por cada sector.

#### Scenario: Desglose por sector
- **WHEN** se solicita el reporte de ocupación de un evento con varios sectores
- **THEN** el sistema devuelve por sector la cantidad y el porcentaje de entradas vendidas, disponibles y devueltas
