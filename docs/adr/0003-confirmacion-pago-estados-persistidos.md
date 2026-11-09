# ADR 0003: Confirmación de pago con estados persistidos (sin transacción distribuida)

- **Fecha:** 2026-11-08
- **Estado:** Aceptado
- **Microservicio:** `compras-service`

## Contexto

Confirmar un pago involucra a `compras-service`, a la pasarela y a `entradas-service`, cada uno con
su propio sistema o base: no existe una transacción que abarque a los tres. El cliente puede
reintentar si se corta la conexión y cualquier paso puede fallar a mitad de camino. La consigna
exige que una entrada no se venda dos veces ni siquiera ante reintentos de pago.

## Decisión

1. `confirmarPago` no es transaccional: las llamadas HTTP a la pasarela y a entradas se hacen fuera
   de toda transacción de base de datos.
2. Cada cambio de estado se guarda en su propia transacción (`CompraTransaccionService`,
   `REQUIRES_NEW`) y queda confirmado antes de seguir.
3. Estados de la orden: `RESERVADA_TEMPORAL` → `PROCESANDO_PAGO` → `PAGO_CONFIRMADO` → `COMPLETADA`,
   con salidas `CANCELADA` (pago rechazado) y `EXPIRADA` (reserva vencida).
4. El pago aprobado se guarda (id de transacción y bloqueos en `VENDIDO`) antes de emitir las
   entradas. Si la emisión falla, la orden queda en `PAGO_CONFIRMADO` y un reintento no vuelve a
   cobrar: solo reintenta la emisión, que es idempotente.
5. Un rechazo cancela la orden y libera los asientos de inmediato.
6. Si la pasarela falla, la orden vuelve a `RESERVADA_TEMPORAL` para poder reintentar antes de vencer.

## Alternativas consideradas

| Alternativa | Motivo para descartarla |
|-------------|--------------------------|
| Transacción distribuida (2PC) | Acopla la disponibilidad de los tres servicios; no es práctica sobre HTTP. |
| Una sola `@Transactional` para todo el flujo | Retiene una conexión de base durante llamadas de red, y una excepción deshace cambios que había que conservar (por ejemplo, liberar asientos tras un rechazo). |
| Compensación automática (reintegro si falla la emisión) | Es lo correcto a largo plazo; se posterga a la Entrega 2 (saga). Por ahora se avanza hacia adelante reintentando la emisión. |

## Consecuencias

- **Positivas:** un rechazo libera los asientos de verdad; un reintento no cobra dos veces; no se
  retienen conexiones de base durante llamadas de red.
- **Negativas / pendientes:**
  - Dos confirmaciones simultáneas pueden pasar juntas el chequeo de estado; hace falta bloquear
    la fila al pasar a `PROCESANDO_PAGO`.
  - Una caída entre estados puede dejar la orden en `PROCESANDO_PAGO` con asientos trabados;
    requiere un proceso de recuperación (Entrega 2).
  - Si la emisión sigue fallando, el cliente pagó y no tiene entradas; no hay reintegro automático.
  - Los estados nuevos obligan a recrear las bases existentes (Hibernate no actualiza la
    restricción de la columna `estado`).

