# Design

## Context

La entrega 1 ya tiene catálogo MongoDB, bloqueo anti-doble venta, pago simulado y emisión de QR con hash (`docs/adr/0001`–`0004`). El pull del equipo agregó JWT/RBAC en `api-gateway` (`AuthController`, `JwtTokenProvider`, `JwtAuthenticationFilter`, `SecurityConfig`, roles `ROLE_*` y usuarios precargados), documentado en `README.md:647` y `docs/openapi.yaml:341`; sin embargo todavía no propaga la identidad a los servicios aguas abajo ni cubre los endpoints nuevos. Los clientes HTTP siguen siendo `RestClient` apuntando a `localhost`, `docker-compose.yml` solo levanta MongoDB y PostgreSQL, y no hay broker, caché, métricas centralizadas ni prueba de carga.

Stack actual: Java 17 + Spring Boot `4.1.2` (según los `pom.xml`), Spring Cloud Gateway WebFlux en `api-gateway`, MongoDB, PostgreSQL, Maven. El usuario pidió una solución directa y explicable en defensa oral: se prefieren clases y endpoints explícitos antes que frameworks de orquestación o patrones distribuidos complejos. Ver `proposal.md` para la motivación de negocio.

## Goals / Non-Goals

**Goals:**

- Cubrir R6.1–R12.2 y los cuatro bloques no funcionales con código implementable y verificable.
- Mantener la consistencia dinero/entradas/asientos con compensaciones explícitas y reconciliación.
- Reutilizar el JWT/RBAC existente en lugar de crear un segundo servicio de autenticación.
- Permitir demostrar en vivo: login por rol, compra con fila, validación de QR, devolución, cancelación, logs correlacionados, caché y carga.
- Dejar `tasks.md` lo suficientemente detallado para programar sin decidir arquitectura sobre la marcha.

**Non-Goals:**

- No implementar Transactional Outbox, saga persistente con motor externo ni DLQ de RabbitMQ.
- No crear `auth-service` ni `db_auth`: el login ya resuelto en `api-gateway` se considera definitivo.
- No integrar OAuth2/OpenID externo ni pasarelas de pago reales.
- No reemplazar los bloqueos de `docs/adr/0001` y `docs/adr/0004` por caché.
- No introducir Promtail ni agentes adicionales de logging.
- No migrar los datos actuales a nuevos esquemas.

## Decisions

### D1. Límites de servicios y persistencia

| Servicio | Responsabilidad | Persistencia | Puerto |
|---|---|---|---|
| `api-gateway` | Rutas, correlación, JWT/RBAC (WebFlux), propaga `X-User-Id`/roles | Sin BD | 8080 |
| `eventos-service` | Catálogo, lugares, sectores, publicación y cancelación | MongoDB `db_eventos` | 8081 |
| `compras-service` | Reservas, órdenes, saga, fila virtual, recaudación | PostgreSQL `db_compras` | 8082 |
| `entradas-service` | Emisión, QR, validación en puerta, devoluciones, ocupación | PostgreSQL `db_entradas` | 8083 |
| `pasarela-mock-service` | `/api/pagos/procesar`, `/api/pagos/reembolsar`, consulta de estado | Sin BD (memoria) | 8084 |

Infraestructura compartida: Redis (caché + fila + dedupe), RabbitMQ, Loki, Grafana y MailHog. Se conserva `db_ticketya` en `init-scripts/` por compatibilidad con datos locales, pero los servicios nuevos usan `db_compras` y `db_entradas`. Camino elegido: un solo contenedor PostgreSQL con dos bases nuevas; alternativa descartada: dos contenedores PostgreSQL (más pesada para defensa local).

ADR: `docs/adr/0005-consistencia-saga-compensaciones.md`.

### D2. Saga de compra orquestada, sin Outbox

`CompraSagaService` vive en `compras-service`; no se agrega un servicio orquestador. Flujo normal:

1. `POST /api/compras/reservar` con `Idempotency-Key`, `X-User-Id` y `paseId` opcional.
2. Si el evento está en modo alta demanda, validar `GET /api/fila/validar?paseId=...` en Redis. Sin pase → 409 y R4.1 modificado.
3. Crear orden + bloqueos con vencimiento de 10 minutos (ADR 0001/0004). Estado inicial `PENDIENTE`.
4. `POST http://pasarela-mock-service:8084/api/pagos/procesar` → orden `PROCESANDO_PAGO`, guardar `paymentId`.
5. `POST http://entradas-service:8083/api/entradas/emitir` → orden `CONFIRMADA`, guardar `ordenCompraId`.
6. Publicar eventos asíncronos (D3) y responder.

Compensación en un solo `catch`:

- Si hay `paymentId`: `POST /api/pagos/reembolsar` en pasarela-mock.
- `POST /api/compras/asientos/liberar` (endpoint ya existente) para cada asiento/cupo.
- Si se emitieron entradas: `DELETE /api/entradas/orden/{ordenCompraId}` (nuevo endpoint de compensación).
- Orden queda `COMPENSADA` con `motivoCompensacion` y se publica `pago.compensado`.

`OrderReconciliationJob` (`@Scheduled(fixedDelay=30000)`) consulta órdenes en `PROCESANDO_PAGO` con más de 2 minutos y llama a `GET /api/pagos/estado/{paymentId}`; converge a `CONFIRMADA` o ejecuta la compensación. Alternativa descartada: Outbox + worker, por su complejidad. Riesgo aceptado: si RabbitMQ falla después del commit local, el evento de notificación se pierde; el dinero y las entradas sí se reconcilian.

### D3. Mensajería asíncrona con RabbitMQ

- Exchange `ticketya.topic` (`topic`), colas `notificaciones.q`, `metricas.q`, `invalidacion-cache.q`, cada una con `@QueueBinding`.
- Routing keys: `pago.aprobado`, `entrada.emitida`, `devolucion.aprobada`, `evento.cancelado`, `evento.modificado`, `pago.compensado`.
- `DomainEventPublisher` usa `RabbitTemplate` + `Jackson2JsonMessageConverter`; se dispara con `@TransactionalEventListener(phase = AFTER_COMMIT)` y `@Async("eventPublisherExecutor")` para no bloquear la respuesta. Sin *publisher confirms* (decisión simple, asumida en ADR).
- Idempotencia en consumidores: clave Redis `seen:{cola}:{eventId}` con TTL 24 h; si ya existe, se hace `ack` sin procesar.
- Reintento: `SimpleRetryPolicy(maxAttempts=3)` + `FixedBackOffPolicy(1000)` por cola. Al agotarse se registra log ERROR con `eventId` y payload, se hace `ack` y no se publica a DLQ.
- Alternativa descartada: mensajería síncrona para correos (acopla la latencia de la compra al SMTP).

ADR: `docs/adr/0010-mensajeria-asincrona.md`.

### D4. Resiliencia con Resilience4j + timeouts

| Dependencia | Reintentos | Timeout | CircuitBreaker | Fallback |
|---|---|---|---|---|
| pasarela-mock | 3 | 2 s | 50% sobre 10 llamadas, 10 s abierto | `PaymentFallback`: orden `PAGO_PENDIENTE`, sin nuevo cargo |
| entradas-service | 3 | 2 s | 50%, 10 s | `TicketFallback`: ejecuta compensación de la saga |
| eventos-service (desde gateway) | 2 | 1,5 s | 50%, 15 s | Catálogo vacío + mensaje "servicio temporalmente no disponible" |

Implementación: `@Retry`, `@TimeLimiter` (métodos que devuelven `CompletableFuture`), `@CircuitBreaker(fallbackMethod = "...")` sobre los clientes `WebClient`. Los `WebClient` se construyen con `.connectTimeout(...)` y `.responseTimeout(...)`. Se migran los `RestClient.create("http://localhost:...")` actuales de `compras-service`, `entradas-service` y `eventos-service` a beans inyectados con URL por configuración. La clave `Idempotency-Key` ya presente en `/api/compras/reservar` se reutiliza en pagos y emisión.

ADR: `docs/adr/0006-tolerancia-a-fallos.md`.

### D5. Correlación, logs y health

- En `api-gateway` (WebFlux) se agrega un `WebFilter` de correlación junto al filtro JWT; en los servicios servlet se usa `OncePerRequestFilter`. Lee `X-Correlation-ID`; si falta, genera UUID; lo pone en MDC (`correlationId`, `service`) y en la respuesta. Limpia MDC en `finally`.
- El productor copia el `correlationId` del MDC a los headers del mensaje Rabbit; el consumidor lo vuelve a poner en MDC antes de procesar.
- `logback-spring.xml` usa `logstash-logback-encoder` para emitir JSON en stdout: `timestamp`, `level`, `service`, `correlationId`, `message`.
- `docker-compose.yml` configura en cada servicio:

```yaml
logging:
  driver: loki
  options:
    loki-url: "http://loki:3100/loki/api/v1/push"
```

- Grafana tiene un datasource Loki (`http://loki:3100`) y un dashboard simple con filtro por `service` y búsqueda por `correlationId`. Health: `/actuator/health` expuesto en los cinco servicios.

ADR: `docs/adr/0008-bitacora-centralizada.md`.

### D6. Seguridad JWT y roles (reutilización del gateway)

- Se conservan los archivos ya traídos: `AuthController` (`POST /api/auth/login`), `JwtTokenProvider` (HS256, `jwt.secret`, expiración 1 h), `JwtAuthenticationFilter` y `SecurityConfig` de `api-gateway`. **No se crea `auth-service` ni `db_auth`.**
- Roles exactos del código actual: `ROLE_CLIENTE`, `ROLE_ORGANIZADOR`, `ROLE_ADMINISTRADOR`, `ROLE_PERSONAL_ACCESO` (`docs/.../Rol.java`). Los nuevos requisitos usan estos mismos nombres.
- El `JwtAuthenticationFilter` además muta el request para agregar `X-User-Id` (el `subject`) y `X-User-Roles`. Los servicios leen esas cabeceras mediante un filtro simple; no se agrega validación JWT duplicada ni `@PreAuthorize` en cada servicio.
- Matriz existente en `SecurityConfig` (ampliarla con los endpoints nuevos):
  - Público: `GET /api/eventos/publicados/**`, `/api/auth/login`, Swagger.
  - `ROLE_CLIENTE`: `/api/compras/**`, `GET /api/entradas/cliente/**`, `POST /api/entradas/{id}/devolucion`, `/api/fila/**`.
  - `ROLE_PERSONAL_ACCESO`: `POST /api/entradas/validar-qr` (endpoint ya documentado en `docs/openapi.yaml:318`).
  - `ROLE_ADMINISTRADOR|ROLE_ORGANIZADOR`: publicar/cancelar eventos, `/api/entradas/devoluciones/**/aprobar|rechazar`, `/api/compras/metricas/**`.
  - `ROLE_ADMINISTRADOR`: compensación manual `POST /api/compras/{id}/compensar`.
- El `clienteId` de reservas, devoluciones y consultas se toma de `X-User-Id`; el parámetro `@RequestParam clienteId` se mantiene solo por compatibilidad y se ignora si difiere.
- Usuarios de prueba ya precargados en `UsuarioRepository`: `admin/admin123`, `organizador1/org123`, `cliente1/cliente123`, `puerta1/puerta123`.

ADR: `docs/adr/0009-seguridad-jwt.md` (reescrito para esta decisión).

### D7. Redis: caché, fila virtual y dedupe

- Caché: `@Cacheable(value="catalogo", key="#id")` sobre `EventoController`/servicio de lectura; `RedisCacheManager` con TTL 60 s. Invalidación explícita al publicar/modificar/cancelar y consumidor de `evento.modificado`/`evento.cancelado` en `invalidacion-cache.q`. `CacheErrorHandler` personalizado: ante `RedisConnectionFailureException` consulta MongoDB y no falla la petición.
- Fila virtual: `ZADD queue:{eventoId}` con score = timestamp, `GET /api/fila/turno` calcula posición con `ZRANK`; `QueueScheduler` (`@Scheduled` cada 1 s) promueve a los primeros y crea `SET pass:{paseId} clienteId EX 600`. `compras-service` valida la existencia de la clave antes de reservar. Modo de alta demanda: configurable por evento (`modoFila: NORMAL|ALTA_DEMANDA`) con umbral de concurrencia.
- Dedupe de eventos: `seen:{cola}:{eventId}` TTL 24 h (ver D3).
- Alternativa descartada: tablas en PostgreSQL para la fila; más lentas y con más limpieza.

ADR: `docs/adr/0007-cache-redis.md` (caché, fila y dedupe en el mismo Redis).

### D8. Devoluciones, rechazo y cancelación masiva

- Ya existe: `POST /api/entradas/{id}/devolucion` (valida 72 h y estado), `POST /api/entradas/devoluciones/{solicitudId}/aprobar` (cancela entrada, libera asiento vía `ComprasClient`, solicita reembolso vía `PasarelaClient`).
- Ya existe parcialmente: `POST /api/eventos/{id}/cancelar` → `POST /api/entradas/eventos/{eventoId}/cancelar`, que cancela entradas y libera asientos; falta agregar el reintegro por orden y los eventos asíncronos.
- Nuevo: `POST /api/entradas/devoluciones/{solicitudId}/rechazar` con `{ motivo }`; estado `RECHAZADA`, entrada sigue `EMITIDA` (R7.2).
- Se amplía la cancelación para que por cada orden afectada llame a `/api/pagos/reembolsar` (R8.2) y publique `evento.cancelado` + `pago.compensado`. Se agrega `Idempotency-Key` derivada de `eventoId + ordenId` para no reintegrar dos veces.
- `POST /api/entradas/validar-qr` recibe `qrHash`, localiza la entrada, exige estado `EMITIDA`, evento vigente y la cambia a `USADA` con `@Version` / bloqueo optimista para el doble escaneo concurrente.

### D9. Docker, compose y prueba de carga

- Un Dockerfile multi-stage por servicio (Maven build + `eclipse-temurin:17-jre`) para los cinco servicios Spring, más un `nginx` opcional para `frontend/`.
- `docker-compose.yml` agrega: 5 servicios Spring, Redis (`redis:7-alpine`), RabbitMQ (`rabbitmq:3-management`), Loki (`grafana/loki:2.9`), Grafana (`grafana/grafana`) y MailHog (`mailhog/mailhog`) para ver correos en local, además de MongoDB/PostgreSQL. Healthchecks y `depends_on` con `condition: service_healthy`.
- `tests/load/jmeter/entrega2.jmx` con dos hilos: (1) consulta `/api/eventos/publicados`, (2) flujo login → reserva con pase → confirmación. Parámetros `HOST`, `USUARIOS`, `RAMP_UP` por propiedades JMeter.
- Ejecución: `jmeter -n -t tests/load/jmeter/entrega2.jmx -l tests/load/jmeter/results.jtl -e -o tests/load/jmeter/report`; el reporte HTML se deja en `tests/load/jmeter/report/`.
- Aceptación oral: explicar qué significa throughput, error % y p95, y comparar con/sin caché.

ADR: `docs/adr/0011-contenedores-y-prueba-de-carga.md`.

## Risks / Trade-offs

- **Evento perdido si RabbitMQ cae tras el commit:** aceptado; se mitiga con logs, reconciliación de órdenes y compensación manual. No hay Outbox (ADR 0005).
- **Sin DLQ:** los mensajes agotados quedan solo en logs de Loki; se acepta porque el objetivo es simplicidad y la intervención es manual (ADR `docs/adr/0010-mensajeria-asincrona.md`).
- **`@TimeLimiter` requiere methods asíncronos:** se implementa con `CompletableFuture` en los clientes; se documenta en el código del cliente para evitar confusiones en defensa.
- **Varios `RestClient` migrados a `WebClient`:** cambio mecánico pero con riesgo de romper pruebas existentes; se cubre con los tests de cada fase.
- **Redis como caché, fila y dedupe:** un solo servicio para tres usos; si cae, la caché degrada a BD y la fila debe degradar a modo normal para no bloquear compras.
- **Identidad propagada por cabeceras:** el gateway es el único que valida el JWT; las cabeceras `X-User-Id`/roles se consideran confiables dentro de la red interna. En producción conviene defensa en profundidad.
- **Usuarios en memoria:** `UsuarioRepository` precarga cuatro usuarios; suficiente para examen, no para producción.
- **Carga con JMeter sobre contenedores locales:** los números no son representativos de producción; se usa solo como evidencia de comportamiento y del efecto de la caché.

## Open Questions

Ninguna. Las dudas sobre la herramienta de bitácora (Loki + Grafana vía *Docker logging driver*, sin Promtail) y sobre la ubicación del login (gateway, sin `auth-service`) quedaron resueltas.
