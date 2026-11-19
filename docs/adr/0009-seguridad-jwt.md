# ADR 0009: Seguridad JWT/RBAC centralizada en api-gateway

- **Fecha:** 2026-11-18
- **Estado:** Aceptado
- **Microservicio:** `api-gateway`

## Contexto

El equipo ya implementó `POST /api/auth/login`, `JwtTokenProvider`, `JwtAuthenticationFilter` y `SecurityConfig` en `api-gateway`, con usuarios precargados y roles `ROLE_CLIENTE`, `ROLE_ORGANIZADOR`, `ROLE_ADMINISTRADOR` y `ROLE_PERSONAL_ACCESO`. Agregar otro servicio de autenticación duplicaría el login y dejaría dos matrices de roles incompatibles.

## Decisión

Reutilizar el JWT/RBAC existente. El gateway valida el token, aplica la matriz de rutas y propaga `X-User-Id` y `X-User-Roles` a los servicios. Los servicios toman el `clienteId` de `X-User-Id` y no validan el JWT por segunda vez. No se crea `auth-service` ni `db_auth`.

**Alternativa descartada:** nuevo `auth-service` con base propia (duplicación de login, filtros y roles).

## Consecuencias

- Un solo login y una sola matriz de roles, alineados con `README.md` y `docs/openapi.yaml`.
- Endpoint de puerta unificado: `POST /api/entradas/validar-qr`.
- La identidad viaja en cabeceras internas de confianza; en producción conviene defensa en profundidad.
