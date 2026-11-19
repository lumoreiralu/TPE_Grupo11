# ADR 0008: Bitácora centralizada con X-Correlation-ID y Loki/Grafana

- **Fecha:** 2026-11-18
- **Estado:** Aceptado
- **Microservicio:** Todos

## Contexto

Una compra produce logs en varios servicios; sin un identificador común es imposible reconstruir el flujo en la defensa o en una incidencia.

## Decisión

`CorrelationFilter` genera o conserva `X-Correlation-ID`, lo pone en MDC y lo propaga por HTTP y RabbitMQ. Los logs se emiten en JSON y Docker los envía a Loki con el *logging driver* `loki`; Grafana consulta por `service` y `correlationId`. Se exponen `/actuator/health`.

**Alternativa descartada:** Promtail + volúmenes de log (un proceso más sin beneficio para el alcance).

## Consecuencias

- Traza completa de una petición con una sola búsqueda en Grafana.
- La configuración depende del daemon de Docker con soporte de `loki`.
