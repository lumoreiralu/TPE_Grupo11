# ADR 0001: Bloqueo anti-doble venta mediante índice único en `bloqueos_asientos`

- **Fecha:** 2026-11-08
- **Estado:** Aceptado
- **Microservicio:** `compras-service`

## Contexto

Varios clientes pueden intentar reservar el mismo asiento de un evento de forma
simultánea. `compras-service` necesita garantizar que una única solicitud gane la
reserva, incluso si el servicio se ejecuta con múltiples instancias y sin un
coordinador externo. La reserva es temporal (10 minutos) y, al vencer, el asiento
debe quedar disponible para otros clientes.

## Decisión

Usar la base de datos PostgreSQL como punto único de coordinación mediante una
restricción de unicidad sobre la columna `clave_asiento` de la tabla
`bloqueos_asientos`.

- `clave_asiento` se compone de `eventoId|sectorId|fila|asiento`, por lo que la
  unicidad aplica al asiento dentro de un evento.
- Al reservar, se inserta un registro por cada asiento con `estado = BLOQUEADO`
  y `expira_en = fechaExpiracionReserva`. La inserción se hace con
  `saveAndFlush` dentro de la misma transacción que crea la orden.
- Si dos transacciones compiten por el mismo asiento, PostgreSQL permite que
  solo una confirme la inserción; la otra recibe una violación de unicidad. El
  servicio la traduce a `409 Conflict` y revierte la orden completa.
- Un bloqueo con `estado = VENDIDO` permanece en la tabla y sigue ocupando la
  clave, por lo que el asiento no puede volver a reservarse.
- Al vencer una orden `RESERVADA_TEMPORAL`, sus bloqueos se eliminan y el
  asiento vuelve a estar disponible. Esto lo hace `liberarReservasVencidas`
  (al iniciar una reserva y por el scheduler cada 30 segundos).
- La `Idempotency-Key` de la orden evita que un reintento del cliente genere
  una segunda orden o un segundo bloqueo.

## Alternativas consideradas

| Alternativa | Motivo para descartarla |
|-------------|--------------------------|
| Bloqueo pesimista (`SELECT ... FOR UPDATE`) | Serializa las reservas por asiento y retiene conexiones/locks durante la llamada a `eventos-service`, reduciendo el throughput y complicando el manejo de timeouts. |
| Bloqueo optimista con `@Version` | Requiere reintentos a nivel de aplicación y no impide por sí solo que dos transacciones lean el asiento como libre antes de insertar. |
| Lock distribuido (Redis/Zookeeper) | Agrega infraestructura y un punto de fallo adicional para un problema que la base de datos ya resuelve de forma transaccional. |
| Verificar disponibilidad con un `SELECT` previo | Es susceptible a condiciones de carrera: entre la lectura y la escritura otra transacción puede insertar el mismo asiento. |

## Consecuencias

- **Positivas:** la garantía de no doble venta queda en la base de datos y
  funciona con múltiples instancias sin coordinación adicional; la transacción
  revierte la orden completa si falla cualquier asiento.
- **Negativas:** la restricción de unicidad debe existir en el esquema de
  producción (Hibernate la crea con `ddl-auto=update` en desarrollo); en una
  migración real conviene crearla explícitamente. Los conflictos se detectan al
  hacer flush, por lo que el servicio depende de que la violación se propague
  correctamente y de que la transacción se revierta.
- **Pruebas:** `ReservaServiceTest.soloUnaReservaGanaElMismoAsientoEnParalelo`
  dispara dos reservas simultáneas sobre el mismo asiento y verifica que solo
  una prospera y la otra recibe `409 Conflict`.
