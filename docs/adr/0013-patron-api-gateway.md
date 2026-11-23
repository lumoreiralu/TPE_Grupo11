# ADR 0012: Implementación del patrón API Gateway como punto de entrada único

- **Fecha:** 2026-11-22
- **Estado:** Aceptado
- **Microservicio:** api-gateway

## Contexto

La plataforma cuenta con múltiples microservicios que exponen sus funcionalidades a través de distintos puertos (`:8081`, `:8082`, `:8083`, `:8084`). Esta distribución requiere que los clientes conozcan la ubicación de cada servicio y puede generar duplicación de la lógica de autenticación y control de acceso.

## Decisión

Implementar el patrón **API Gateway** mediante el servicio `api-gateway` (`:8080`) como único punto de entrada para las aplicaciones cliente (Frontend React, Postman y clientes móviles).

El Gateway centraliza la validación de tokens JWT mediante `JwtAuthenticationFilter` y aplica el control de acceso basado en roles (RBAC), contemplando permisos como `ROLE_CLIENTE` y `ROLE_ORGANIZADOR`.

Una vez validada la identidad del usuario, propaga las cabeceras internas correspondientes, como `X-User-Id` y `X-User-Roles`, hacia los microservicios backend, evitando duplicar responsabilidades de autenticación y manteniendo el desacoplamiento entre los servicios.

**Alternativa descartada:** permitir que los clientes consuman directamente cada microservicio y que cada uno implemente su propia lógica de autenticación y autorización (mayor acoplamiento, exposición de la topología interna y duplicación de código).

## Consecuencias

- Punto de entrada único (`:8080`), que abstrae los puertos y las direcciones internas de los microservicios.
- Centralización de la seguridad, validación JWT y autorización mediante roles.
- Propagación transparente de la identidad del usuario hacia los servicios backend.
- Menor duplicación de código y responsabilidades de seguridad en los microservicios.
- El API Gateway se convierte en un componente crítico: si deja de funcionar, los clientes pierden el acceso a los servicios de la plataforma.