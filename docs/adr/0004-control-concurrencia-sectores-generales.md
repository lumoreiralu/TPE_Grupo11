# ADR 0004: Decremento atómico de cupos en sectores no numerados (Campo)

- **Fecha:** 2026-11-10
- **Estado:** Aceptado
- **Microservicio:** `compras-service`

## Contexto

Los sectores no numerados o de "Campo" poseen una capacidad máxima general sin asignación de fila ni asiento. Varios clientes pueden competir simultáneamente por los últimos lugares disponibles de un sector general. Se debe asegurar que:
1. No se sobrevenda el último lugar bajo compras simultáneas.
2. La verificación y el descuento del cupo disponible sea atómico a nivel de base de datos.
3. El cupo se restaure de inmediato si la reserva vence o el pago es rechazado.

## Decisión

1. **Tabla `cupos_sectores`:** Se crea en la base relacional de `compras-service` con columnas `(evento_id, sector_id, capacidad_total, cupos_disponibles)` y restricción de unicidad sobre `(evento_id, sector_id)`.
2. **Decremento atómico vía UPDATE:** Se utiliza una sentencia SQL atómica:
   ```sql
   UPDATE cupos_sectores 
   SET cupos_disponibles = cupos_disponibles - :cantidad 
   WHERE evento_id = :eventoId 
     AND sector_id = :sectorId 
     AND cupos_disponibles >= :cantidad
   ```
   PostgreSQL adquiere un bloqueo exclusivo a nivel de fila durante la actualización. Si `cupos_disponibles < :cantidad`, la sentencia afecta 0 filas, lo que se traduce de inmediato en una excepción de negocio `409 Conflict`.
3. **Restauración de cupo:** Si una reserva vence o el pago resulta rechazado, se ejecuta:
   ```sql
   UPDATE cupos_sectores 
   SET cupos_disponibles = LEAST(capacidad_total, cupos_disponibles + :cantidad) 
   WHERE evento_id = :eventoId AND sector_id = :sectorId
   ```
4. **Registro en `bloqueos_asientos`:** Para mantener trazabilidad y control de expiración homogéneo, cada entrada general reservada se registra con una clave determinista única por orden (`eventoId|sectorId|GENERAL|ordenId|index`), la cual se elimina al expirar o se marca como `VENDIDO` al confirmarse el pago.

## Alternativas consideradas

| Alternativa | Motivo para descartarla |
|-------------|--------------------------|
| Conteo dinámico con `COUNT(*)` sobre reservas activas | Vulnerable a condiciones de carrera ("phantom reads") bajo concurrencia alta; costoso en I/O. |
| Bloqueo optimista (`@Version`) | Provoca alta tasa de fallos y reintentos innecesarios cuando muchos clientes compiten por el mismo recurso. |
| Redis / semáforo distribuido | Complejidad de infraestructura y riesgo de inconsistencias con la base de datos relacional. |

## Consecuencias

- **Positivas:**
  - Garantía matemática y transaccional contra la sobreventa del último lugar.
  - Alto rendimiento delegando la serialización al motor de base de datos PostgreSQL.
  - Liberación y restitución automática de cupos ante cancelaciones o reservas vencidas.
- **Negativas:**
  - Contención concentrada en la fila del sector durante picos masivos de venta (mitigado por la rapidez de la actualización in-place).

