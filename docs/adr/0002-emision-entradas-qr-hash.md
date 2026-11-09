# ADR 0002: Código QR de entradas como token aleatorio único

- **Fecha:** 2026-11-08
- **Estado:** Aceptado
- **Microservicio:** `entradas-service`

## Contexto

Cada entrada emitida tras un pago confirmado necesita un código único e imposible de adivinar,
que se usará para validarla en el acceso al evento. `entradas-service` tiene su propia base
(Database per Service) y `compras-service` puede reintentar la emisión de una orden.

## Decisión

1. **Generación:** el código es el SHA-256 (hexadecimal, 64 caracteres) de una cadena que incluye
   datos de la entrada y un `UUID.randomUUID()`. La imposibilidad práctica de adivinarlo proviene de
   ese UUID (generador criptográficamente seguro); el hash es un formato uniforme, no una firma.
2. **Unicidad:** la columna `codigo_qr_hash` tiene restricción `UNIQUE` en `db_entradas`.
3. **Idempotencia:** `POST /api/entradas/emitir` devuelve las entradas ya emitidas si la orden
   ya las tiene. Esto cubre los reintentos secuenciales.
4. **Validación (Entrega 2):** cuando se implemente el control de acceso, se buscará el código por
   índice (`findByCodigoQrHash`) y se revisará el estado de la entrada. Hoy solo existe la emisión.

## Alternativas consideradas

| Alternativa | Motivo para descartarla |
|-------------|--------------------------|
| `UUID` simple como código | Es igual de impredecible. Se prefirió el hash por tener formato uniforme de 64 caracteres; la diferencia de seguridad es nula. |
| HMAC-SHA256 con clave secreta | Permitiría verificar autenticidad sin consultar la base, pero el estado (usada/devuelta) igual exige consultarla. Se posterga a la Entrega 2. |
| Imagen PNG del QR en el servicio | Guardar imágenes pesa en la base; el cliente puede dibujar el QR a partir del código. |

## Consecuencias

- **Positivas:** códigos prácticamente imposibles de adivinar; unicidad garantizada por la base;
  reintentos secuenciales de `compras-service` no duplican entradas.
- **Negativas / pendientes:**
  - Dos emisiones simultáneas de la misma orden podrían crear entradas duplicadas: falta una
    restricción única por (orden, sector, fila, asiento).
  - La validación del QR depende de la base, y debe verificar el estado de la entrada
    (EMITIDA, USADA, DEVUELTA, CANCELADA), en particular tras una devolución.
  - `compras-service` y `entradas-service` quedan acoplados por el contrato de emisión.