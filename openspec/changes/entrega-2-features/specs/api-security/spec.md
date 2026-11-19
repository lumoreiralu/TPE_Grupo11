# Spec Delta

## Purpose

Protege los endpoints con autenticación JWT y autorización por roles, garantizando que la identidad del cliente provenga del token y no de parámetros manipulables por el llamador.

## ADDED Requirements

### Requirement: Autenticación obligatoria (SEC-1)
When una petición llega a un endpoint protegido sin credenciales, the system SHALL denegar el acceso con respuesta 401 antes de ejecutar la operación.

#### Scenario: Petición sin token
- **WHEN** un llamador invoca un endpoint protegido sin cabecera de autorización
- **THEN** el sistema responde 401 y no ejecuta la operación

### Requirement: Rechazo de credenciales inválidas (SEC-2)
If el token presentado está expirado o su firma no es válida, then the system SHALL denegar el acceso con respuesta 401.

#### Scenario: Token expirado
- **WHEN** un llamador presenta un token JWT vencido
- **THEN** el sistema responde 401 e informa que la sesión expiró

#### Scenario: Firma inválida
- **WHEN** un llamador presenta un token con firma modificada
- **THEN** el sistema responde 401 y no confía en los datos del token

### Requirement: Autorización por rol (SEC-3)
When un usuario autenticado ejecuta una operación, the system SHALL verificar que su rol habilite esa operación y responder 403 cuando no sea así.

#### Scenario: Cliente intenta aprobar una devolución
- **WHEN** un usuario con rol `ROLE_CLIENTE` intenta aprobar una solicitud de devolución
- **THEN** el sistema responde 403 y no modifica la solicitud

#### Scenario: Personal de acceso valida un QR
- **WHEN** un usuario con rol `ROLE_PERSONAL_ACCESO` invoca `POST /api/entradas/validar-qr`
- **THEN** el sistema autoriza la validación del QR en la puerta

#### Scenario: Administrador cancela un evento
- **WHEN** un usuario con rol `ROLE_ADMINISTRADOR` o `ROLE_ORGANIZADOR` cancela un evento
- **THEN** el sistema autoriza la cancelación

### Requirement: Identidad del cliente derivada del token (SEC-4)
When un cliente consulta sus entradas o realiza una reserva, the system SHALL obtener el identificador de cliente de la cabecera `X-User-Id` propagada por el gateway desde el token y rechazar un identificador distinto enviado por el llamador.

#### Scenario: Intento de suplantación
- **WHEN** un cliente envía en la consulta un `clienteId` distinto al de su token
- **THEN** el sistema ignora o rechaza el parámetro y devuelve únicamente los datos del cliente autenticado
