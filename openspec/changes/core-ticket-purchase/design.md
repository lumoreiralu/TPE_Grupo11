# Diseño Arquitectónico y Modelo de Datos 

## 1. Microservicios y Responsabilidades 
**`api-gateway`**: Punto de entrada único (Spring Cloud Gateway) para enrutar tráfico HTTP hacia los microservicios. 
**`eventos-service`**: Administración de recintos, sectores, publicación de eventos y catálogo de búsquedas (Base de Datos: MongoDB / NoSQL).
**`compras-service`**: Ciclo de vida de órdenes, reservas temporales (TTL 10 min) y control de concurrencia anti-doble venta (Base de Datos: PostgreSQL / Relacional).
**`entradas-service`**: Emisión, almacenamiento y validación de entradas digitales con código QR (Base de Datos: PostgreSQL / Relacional).
**`pasarela-mock-service`**: Servicio mock para simulación de cobros y devoluciones. 

## 2. Modelo de Datos por Microservicio
### A. `eventos-service` (MongoDB) 
- **`Lugares`**: `{ _id, nombre, direccion, ubicacion: { lat, lon }, sectores: [{ sector_id, nombre, tipo, capacidad_maxima, filas }] }`
- **`Eventos`**: `{ _id, nombre, descripcion, tipo, organizador_id, fecha_hora, lugar_id, estado, precios_sectores: [{ sector_id, precio }] }` 

### B. `compras-service` (PostgreSQL)  
- **`ordenes_compra`**: `id (UUID PK), cliente_id, evento_id, fecha_creacion, fecha_expiracion_reserva, estado, monto_total, transaccion_pago_id`
- **`detalles_reserva`**: `id (PK), orden_id (FK), sector_id, fila, asiento, precio_unitario` 
- **`bloqueos_asientos`**: `id (PK), evento_id, sector_id, fila_asiento_hash (UNIQUE INDEX), orden_id, estado, expira_en`

### C. `entradas-service` (PostgreSQL) 
- **`entradas`**: `id (UUID PK), orden_compra_id, cliente_id, evento_id, sector_nombre, fila, asiento, codigo_qr_hash (UNIQUE), estado, fecha_emision`

## 3. Diagrama de Secuencia del Flujo de Compra 
```text 
Cliente -> API Gateway: POST /api/compras/reservar (Evento, Asiento)
API Gateway -> compras-service: Validar y aplicar Bloqueo de Concurrencia 
compras-service -> eventos-service: GET /api/eventos/{id} (Validar precio)
compras-service -> Cliente: 201 Created (ID Reserva, Expiración 10 min) 
Cliente -> API Gateway: POST /api/compras/{id}/confirmar-pago 
compras-service -> pasarela-mock: POST /api/pagos/procesar 
pasarela-mock -> compras-service: 200 OK (Pago Aprobado) 
compras-service -> entradas-service: POST /api/entradas/emitir 
entradas-service -> compras-service: Ticket con QR generado 
compras-service -> Cliente: 200 OK (Compra Completada + Ticket)