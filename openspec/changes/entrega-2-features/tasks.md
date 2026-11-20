# Tasks

## 0. ADRs y convenciones

- [x] 0.1 Crear `docs/adr/0005-consistencia-saga-compensaciones.md` (Contexto / Decisión / Consecuencias): saga síncrona con `try/catch`, sin Outbox, riesgo de evento perdido asumido.
- [x] 0.2 Crear `docs/adr/0006-tolerancia-a-fallos.md`: Resilience4j + timeouts + idempotencia, sin motor externo.
- [x] 0.3 Crear `docs/adr/0007-cache-redis.md`: Redis para caché de catálogo, fila virtual y dedupe; degradación segura.
- [x] 0.4 Crear `docs/adr/0008-bitacora-centralizada.md`: `X-Correlation-ID`, MDC, JSON y Loki/Grafana vía logging driver.
- [x] 0.5 Reescribir `docs/adr/0009-seguridad-jwt.md`: JWT/RBAC centralizado en `api-gateway`, sin `auth-service` ni `db_auth`.
- [x] 0.6 Crear `docs/adr/0010-mensajeria-asincrona.md`: RabbitMQ topic, consumidores idempotentes, reintentos simples, sin DLQ.
- [x] 0.7 Crear `docs/adr/0011-contenedores-y-prueba-de-carga.md`: cinco contenedores de servicio, compose completo y JMeter.
- [x] 0.8 Agregar en `application.yml` de cada servicio las propiedades `app.services.*.url`, `spring.rabbitmq.*`, `spring.data.redis.*` y `management.endpoints.web.exposure.include=health`.
- [x] 0.9 Verificar que `docs/adr/0001`–`0004` siguen vigentes y citarlos desde `design.md`.

## 1. Seguridad JWT y roles (gateway ya implementado)

- [x] 1.1 Reutilizar `AuthController`, `JwtTokenProvider`, `LoginRequest`, `AuthResponse`, `Usuario` y `UsuarioRepository` de `api-gateway`; no crear `auth-service`.
- [x] 1.2 `POST /api/auth/login` valida credenciales BCrypt y devuelve JWT con `sub`, `roles` y expiración de 1 hora.
- [x] 1.3 `JwtTokenProvider` firma HS256 usando `jwt.secret` y expone `validarToken`, `obtenerUserName` y `obtenerRoles`.
- [x] 1.4 `JwtAuthenticationFilter` + `SecurityConfig` (WebFlux) con matriz inicial de rutas públicas y por rol.
- [x] 1.5 Ampliar `JwtAuthenticationFilter` para mutar el request y agregar `X-User-Id` y `X-User-Roles`.
- [x] 1.6 Ampliar `SecurityConfig` con `/api/fila/**`, `/api/compras/metricas/**`, `/api/entradas/devoluciones/*/rechazar` y `/api/compras/{id}/compensar`.
- [x] 1.7 `docs/openapi.yaml` documenta `bearerAuth`, `/api/auth/login` y respuestas 401/403.
- [x] 1.8 Tomar el `clienteId` de `X-User-Id` en reservas, devoluciones y consulta de entradas; ignorar un parámetro distinto.
- [x] 1.9 **Tests:** login correcto, token expirado, firma inválida, rol insuficiente (403), suplantación de `clienteId` y tests existentes de los servicios.

## 2. RabbitMQ y eventos de dominio

- [x] 2.1 Agregar `spring-boot-starter-amqp` y configuración de exchange/colas en los servicios que publican o consumen.
- [x] 2.2 Crear el DTO inmutable `DomainEvent` con `eventId`, `type`, `aggregateId`, `payload`, `correlationId` y `occurredAt`.
- [x] 2.3 Implementar `DomainEventPublisher` con `@TransactionalEventListener(AFTER_COMMIT)` y `@Async("eventPublisherExecutor")`, `RabbitTemplate` y `Jackson2JsonMessageConverter`.
- [x] 2.4 Declarar `ticketya.topic` y las colas `notificaciones.q`, `metricas.q`, `invalidacion-cache.q` con sus `@QueueBinding`.
- [x] 2.5 Implementar consumidores esqueleto en `entradas-service` (notificaciones), `compras-service` (métricas) y `eventos-service` (invalidación de caché).
- [x] 2.6 Agregar dedupe con clave Redis `seen:{cola}:{eventId}` TTL 24 h y `ack` de duplicados.
- [x] 2.7 Configurar `SimpleRetryPolicy(3)` + `FixedBackOffPolicy(1000)` por cola y log ERROR con payload al agotar intentos, sin DLQ.
- [x] 2.8 **Tests:** publicación tras commit, evento duplicado procesado una vez, reintento agotado registrado y publisher asíncrono que no retrasa la respuesta.

## 3. Correlación, logs y health

- [x] 3.1 Agregar `logstash-logback-encoder` y `logback-spring.xml` con JSON en stdout en los cinco servicios.
- [x] 3.2 Implementar correlación: `WebFilter` en `api-gateway` y `OncePerRequestFilter` en los servicios; genera UUID si falta, escribe MDC, propaga header y limpia MDC en `finally`.
- [x] 3.3 Copiar `correlationId` del MDC a los headers de Rabbit en el publisher y restaurarlo en los consumidores.
- [x] 3.4 Exponer `/actuator/health` y verificar que responda `UP` en cada servicio.
- [x] 3.5 **Tests:** petición sin correlation ID genera uno, petición con ID lo conserva, MDC queda limpio y logs incluyen `service` + `correlationId`.

## 4. Saga de compra, resiliencia y compensaciones

- [x] 4.1 Crear beans `WebClient` con `connectTimeout=2s`/`responseTimeout=2s` y migrar los `RestClient.create("http://localhost:...")` de `compras-service`, `entradas-service` y `eventos-service`.
- [x] 4.2 Agregar dependencias Resilience4j y configurar instancias `paymentGateway`, `ticketService` y `eventService` en `application.yml`.
- [x] 4.3 Anotar los clientes con `@Retry`, `@TimeLimiter` (métodos `CompletableFuture`) y `@CircuitBreaker(fallbackMethod=...)`; implementar los fallbacks de `design.md` D4.
- [x] 4.4 Extraer `CompraSagaService` desde `ReservaService.confirmarPago`: reserva → pago → emisión → confirmación en un `try`, compensación en un `catch`.
- [x] 4.5 Agregar `DELETE /api/entradas/orden/{ordenCompraId}` para borrar entradas emitidas durante la compensación.
- [x] 4.6 Agregar `POST /api/compras/{id}/compensar` (rol `ROLE_ADMINISTRADOR`) y reutilizar `POST /api/pagos/reembolsar` de `pasarela-mock-service`.
- [x] 4.7 Agregar `OrderReconciliationJob` cada 30 s para órdenes `PROCESANDO_PAGO` con más de 2 minutos; consultar estado real y converger.
- [x] 4.8 Enviar `Idempotency-Key` en pago y emisión; almacenar resultado en Redis TTL 24 h y devolver el mismo resultado en reintentos.
- [x] 4.9 Publicar `pago.aprobado`, `entrada.emitida` y `pago.compensado` desde la saga.
- [ ] 4.10 **Tests:** emisión fallida reintegra y libera, orden pendiente converge, reintento con misma clave no duplica, circuit breaker usa fallback, `ReservaConcurrenteTest` y `CompraControllerTest` siguen pasando.

## 5. Caché de catálogo con Redis

- [x] 5.1 Agregar `spring-boot-starter-data-redis` y `RedisCacheManager` con TTL 60 s para `catalogo`.
- [x] 5.2 Aplicar `@Cacheable(value="catalogo")` a `GET /api/eventos/publicados`, `GET /api/eventos/{id}` y `GET /api/eventos/buscar`.
- [x] 5.3 Invalidar caché al publicar/modificar/cancelar evento y desde el consumidor de `invalidacion-cache.q`.
- [x] 5.4 Implementar `CacheErrorHandler` que consulte MongoDB cuando Redis falle y registre el aviso.
- [x] 5.5 **Tests:** segunda consulta no accede a MongoDB, TTL vence, invalidación tras cancelación y degradación ante caída de Redis.

## 6. Devoluciones y cancelación masiva (R6–R8)

- [x] 6.1 `solicitarDevolucion` ya valida plazo de 72 h, estado `EMITIDA`, propiedad del cliente y solicitud pendiente duplicada.
- [ ] 6.2 Implementar `POST /api/entradas/devoluciones/{solicitudId}/rechazar` con `{ motivo }`, estado `RECHAZADA` y entrada `EMITIDA` (R7.2).
- [x] 6.3 `aprobarDevolucion` ya cancela la entrada, libera el asiento vía `ComprasClient` y solicita el reembolso vía `PasarelaClient` (R7.1).
- [ ] 6.4 Ampliar `POST /api/entradas/eventos/{eventoId}/cancelar` para reintegrar cada orden afectada con idempotencia derivada de `eventoId + ordenId` (R8.2).
- [ ] 6.5 Publicar `devolucion.aprobada`, `evento.cancelado` y `pago.compensado`; invalidar caché del evento.
- [ ] 6.6 **Tests:** rechazo conserva entrada, aprobación reintegra y libera, cancelación masiva reintegra una sola vez por orden, solicitudes con menos de 72 h se rechazan, `DevolucionServiceTest` y `EntradaServiceTest` siguen pasando.

## 7. Validación de entradas por QR (R9)

- [x] 7.1 Implementar `POST /api/entradas/validar-qr` con rol `ROLE_PERSONAL_ACCESO` (ya documentado, aún sin código).
- [x] 7.2 Buscar la entrada por hash, exigir `EMITIDA` y evento vigente, cambiar a `USADA`, registrar fecha/hora de ingreso y usar `@Version` para doble escaneo concurrente.
- [x] 7.3 Rechazar con 409 reingreso (`USADA`), 404 QR inexistente, 400 QR de otro evento y 400 entrada cancelada.
- [x] 7.4 Registrar cada intento con `correlationId` y rol del validador.
- [x] 7.5 **Tests:** primer ingreso autoriza, dos escaneos simultáneos solo uno pasa, reingreso alerta, QR inválido/ajeno/cancelado se deniegan, `QrCodeGeneratorTest` sigue pasando.

## 8. Fila virtual (R10)

- [x] 8.1 Agregar configuración `modoFila: NORMAL|ALTA_DEMANDA` y umbral de concurrencia por evento.
- [x] 8.2 Implementar en `compras-service`: `POST /api/fila/join` (ZADD en `queue:{eventoId}`), `GET /api/fila/turno` (ZRANK) y `POST /api/fila/validar`.
- [x] 8.3 Implementar `QueueScheduler` cada 1 s que promueve turnos y crea `SET pass:{paseId}` con expiración de 600 s.
- [x] 8.4 Integrar la validación del pase en `CompraSagaService` antes de crear la orden; sin pase responde 409 con instrucción de esperar turno.
- [x] 8.5 Mostrar posición/turno en `frontend/` y exponer los endpoints en `docs/openapi.yaml`.
- [x] 8.6 **Tests:** turnos correlativos, pase expira y pasa al siguiente, reserva sin pase se deniega, reserva con pase vigente continúa.

## 9. Notificaciones (R11)

- [ ] 9.1 Agregar `spring-boot-starter-mail` y `JavaMailSender` apuntando a MailHog en local.
- [ ] 9.2 Implementar `NotificationService.enviarConfirmacion(...)` y `enviarAvisoEvento(...)` con plantilla simple de texto.
- [ ] 9.3 Suscribir `notificaciones.q` a `pago.aprobado`, `devolucion.aprobada`, `evento.cancelado` y `evento.modificado`.
- [ ] 9.4 Aplicar reintento y registro de fallo de `async-messaging` sin afectar la compra.
- [ ] 9.5 **Tests:** correo de confirmación, aviso a todos los compradores del evento, fallo temporal reintenta y no revienta la saga.

## 10. Métricas de recaudación y ocupación (R12)

- [ ] 10.1 Implementar `GET /api/compras/metricas/{eventoId}/recaudacion` sumando órdenes `CONFIRMADA` y descontando `COMPENSADA`/reembolsadas.
- [ ] 10.2 Implementar `GET /api/entradas/metricas/{eventoId}/ocupacion` con vendidas/disponibles/devueltas y porcentaje por sector.
- [ ] 10.3 Documentar ambos endpoints en `docs/openapi.yaml` con rol `ROLE_ADMINISTRADOR|ROLE_ORGANIZADOR`.
- [ ] 10.4 **Tests:** recaudación coincide con órdenes pagadas, ocupación por sector suma el total de entradas, sector sin entradas devuelve 0%.

## 11. Docker, compose y smoke E2E

- [ ] 11.1 Crear un Dockerfile multi-stage para `api-gateway`, `eventos-service`, `compras-service`, `entradas-service` y `pasarela-mock-service`.
- [ ] 11.2 Actualizar `docker-compose.yml` con los cinco servicios, Redis, RabbitMQ, Loki, Grafana, MailHog, MongoDB y PostgreSQL, healthchecks y logging driver `loki`.
- [ ] 11.3 Agregar `db_compras` y `db_entradas` en `init-scripts/` conservando `db_ticketya` (sin `db_auth`).
- [ ] 11.4 Agregar servicio `nginx:alpine` opcional para servir `frontend/` en el puerto 4200.
- [ ] 11.5 Ejecutar `docker compose up -d` y smoke manual: login → catálogo → fila → reserva → pago → QR → devolución → cancelación.
- [ ] 11.6 Verificar UI de RabbitMQ (`:15672`), MailHog (`:8025`) y Grafana (`:3000`) con búsqueda por `correlationId`.
- [ ] 11.7 Actualizar `README.md` con comandos de arranque, roles de prueba y dónde ver logs/correos.

## 12. Verificación final y entrega

- [ ] 12.1 Ejecutar `mvn test` en `api-gateway`, `eventos-service`, `compras-service/compras-service`, `entradas-service` y `pasarela-mock-service`.
- [ ] 12.2 Ejecutar el flujo E2E multi-service completo (con compensación forzada y con fila activa) y registrar capturas en `postman/` o `docs/`.
- [ ] 12.3 Verificar matriz de seguridad con curl/Postman: sin token 401, token vencido 401, `ROLE_CLIENTE` en endpoint admin 403, roles correctos 200.
- [ ] 12.4 Crear `tests/load/jmeter/entrega2.jmx` con escenarios de catálogo (con/sin caché) y compra concurrente; parametrizar `HOST`, `USUARIOS`, `RAMP_UP`.
- [ ] 12.5 Ejecutar `jmeter -n -t tests/load/jmeter/entrega2.jmx -l tests/load/jmeter/results.jtl -e -o tests/load/jmeter/report` y dejar el reporte HTML generado.
- [ ] 12.6 Anotar en `docs/` un resumen de throughput, error % y p95 antes/después de caché para la defensa oral.
- [ ] 12.7 Ejecutar `openspec validate entrega-2-features --strict` y `openspec status --change entrega-2-features`; marcar todas las casillas y confirmar `isPlanningComplete: true`.
