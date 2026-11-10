# Checklist de Implementación (Tasks)

- [x] **Fase 1: Preparación del Repositorio y Entorno**
- [x] Crear el proyecto base de Spring Boot para cada microservicio (`api-gateway`, `eventos-service`, `compras-service`, `entradas-service`, `pasarela-mock-service`).
- [x] Configurar el archivo `docker-compose.yml` para levantar las bases de datos (PostgreSQL y MongoDB).

- [x] **Fase 2: Implementación de `eventos-service`**
- [x] Implementar entidades y repositorios de Lugares y Eventos.
- [x] Crear endpoints REST para ABM de lugares, sectores y publicación de eventos.
- [x] Implementar búsqueda multi-criterio de eventos publicadas.

- [x] **Fase 3: Implementación de `compras-service` (Core anti-doble venta)**
- [x] Diseñar el mecanismo de bloqueo pesimista/optimista o índice único en `bloqueos_asientos`.
- [x] Implementar endpoint de reserva temporal con asignación de vencimiento (10 min).
- [x] Integrar llamada síncrona al `pasarela-mock-service` para procesamiento de pago.

- [x] **Fase 4: Implementación de `entradas-service`**
- [x] Crear generador de código QR hash único.
- [x] Implementar endpoint de emisión de entradas invocado por `compras-service`.

- [ ] **Fase 5: API Gateway e Integración**
- [x] Configurar rutas en `api-gateway` hacia los microservicios backend.
- [ ] Probar el flujo completo de compra de punta a punta.
