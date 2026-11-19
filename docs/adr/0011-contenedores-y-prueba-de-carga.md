# ADR 0011: Contenedores por servicio y prueba de carga con JMeter

- **Fecha:** 2026-11-18
- **Estado:** Aceptado
- **Microservicio:** Todos

## Contexto

Hoy los servicios se ejecutan a mano contra `localhost` y no hay evidencia de comportamiento bajo carga ni de la mejora de la caché.

## Decisión

Un Dockerfile multi-stage por servicio y un `docker-compose.yml` con cinco servicios Spring, MongoDB, PostgreSQL, Redis, RabbitMQ, Loki, Grafana y MailHog. La prueba de carga usa un plan `tests/load/jmeter/entrega2.jmx` no interactivo con reporte HTML, comparando consultas con y sin caché.

**Alternativa descartada:** ejecutar JMeter contra procesos locales (menos reproducible y sin logs centralizados).

## Consecuencias

- Arranque reproducible con `docker compose up -d` y evidencia para la defensa oral.
- Los números de carga son locales, no representativos de producción.
