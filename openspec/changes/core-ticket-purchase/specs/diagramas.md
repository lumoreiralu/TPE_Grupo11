1. Diagrama de Módulos (Componentes del Flujo de Compra)

Este diagrama representa cómo se interconectan los microservicios y bases de datos durante el ciclo de vida de una reserva y compra

```mermaid
graph TD
Client[Cliente / Frontend] -->|Peticiones HTTP/REST| Gateway[API Gateway :8080]

    subgraph Microservicios Core
        Gateway -->|/api/eventos/*| EventosSvc[eventos-service :8081]
        Gateway -->|/api/compras/*| ComprasSvc[compras-service :8082]
        Gateway -->|/api/entradas/*| EntradasSvc[entradas-service :8083]
        
        ComprasSvc -->|Consulta eventos/precios| EventosSvc
        ComprasSvc -->|Solicita cobro / reintegro| PasarelaMock[pasarela-mock-service :8084]
        ComprasSvc -.->|Emite tickets solo si pago OK| EntradasSvc
    end

    subgraph Bases de Datos
        EventosSvc --> Mongo[(MongoDB :27017)]
        ComprasSvc --> PostgreCompras[(PostgreSQL Compras :5432)]
        EntradasSvc --> PostgreEntradas[(PostgreSQL Entradas :5433)]
    end
```
2. Diagrama de Secuencia: Compra con Pago Rechazado

Escenario: El cliente bloquea un asiento correctamente, pero la pasarela de pagos rechaza la transacción (por fondos insuficientes, tarjeta inválida, etc.)  El sistema debe actualizar la orden a RECHAZADA y liberar el asiento de inmediato para no retenerlo innecesariamente

```mermaid
sequenceDiagram
autonumber
actor Cliente
participant Gateway as API Gateway
participant Compras as compras-service
participant DB as PostgreSQL (Compras)
participant Eventos as eventos-service
participant Pasarela as pasarela-mock-service

    %% Paso 1: Reserva exitosa previa
    Cliente->>Gateway: POST /api/compras/reservas (eventoId, sector, asiento)
    Gateway->>Compras: POST /api/compras/reservas
    Compras->>Eventos: GET /api/eventos/{id} (Valida estado y precios)
    Eventos-->>Compras: 200 OK (Evento PUBLICADO)
    
    Compras->>DB: INSERT BloqueoAsiento & Orden (Estado: PENDIENTE, expira: +10 min)
    DB-->>Compras: Guardado OK
    Compras-->>Gateway: 201 Created (ordenId, tiempoRestante: 600s)
    Gateway-->>Cliente: 201 Created (ordenId)

    %% Paso 2: Intento de Pago
    Cliente->>Gateway: POST /api/compras/ordenes/{id}/pagar (datosPago)
    Gateway->>Compras: POST /api/compras/ordenes/{id}/pagar
    
    Compras->>DB: SELECT Orden & Bloqueo WHERE id = {id}
    DB-->>Compras: Orden PENDIENTE (vigente)

    %% Paso 3: Llamada a la Pasarela
    Compras->>Pasarela: POST /api/pagos/cobrar (monto, tarjeta)
    Pasarela-->>Compras: 402 Payment Required (o 400 Bad Request: "Fondos insuficientes")

    %% Paso 4: Compensación / Liberación Inmediata
    critical Liberación inmediata del lugar
        Compras->>DB: UPDATE Orden SET estado = 'RECHAZADA'
        Compras->>DB: DELETE FROM BloqueoAsiento WHERE ordenId = {id}
        DB-->>Compras: Asiento liberado en BD
    end

    Compras-->>Gateway: 400 / 402 (Error: Pago rechazado. Asiento liberado)
    Gateway-->>Cliente: 400 / 402 (Pago rechazado. Puedes reintentar con otro medio)

```
---

3. Diagrama de Secuencia: Reserva Vencida (Timeout de los 10 minutos)

Escenario: El cliente genera la reserva temporal, pero abandona la pantalla o tarda más del tiempo límite (ej. 10 minutos) sin completar el pago 
Aquí intervienen dos mecanismos concurrentes 
1. El Job en segundo plano @Scheduled que barre periódicamente y libera los bloqueos expirados.
2. La validación defensiva al intentar pagar tardíamente*, que rechaza la operación informando que la reserva expiró

```mermaid
sequenceDiagram
autonumber
actor Cliente
participant Gateway as API Gateway
participant Compras as compras-service
participant Scheduler as Tarea Programada (@Scheduled)
participant DB as PostgreSQL (Compras)
actor OtroCliente as Otro Cliente

    %% 1. Creación de la reserva
    Cliente->>Gateway: POST /api/compras/reservas
    Gateway->>Compras: POST /api/compras/reservas
    Compras->>DB: INSERT BloqueoAsiento (asiento: 15, expira: 18:10:00)
    Compras-->>Cliente: 201 Created (Expira a las 18:10:00)

    Note over Cliente, DB: Transcurren más de 10 minutos sin confirmación de pago (reloj marca 18:10:01)

    %% 2. Liberación por Scheduler
    rect rgb(240, 248, 255)
        Note over Scheduler, DB: Barrido automático en background (cada 30s o 1 min)
        Scheduler->>Compras: dispararLiberacionExpiradas()
        Compras->>DB: SELECT Bloqueos WHERE fechaExpiracion < NOW()
        DB-->>Compras: [BloqueoAsiento #15 expirado]
        Compras->>DB: UPDATE Orden SET estado = 'EXPIRADA'
        Compras->>DB: DELETE FROM BloqueoAsiento WHERE id = #15
        DB-->>Compras: Asiento 15 liberado y disponible
    end

    %% 3. Otro cliente puede reservar el asiento liberado
    OtroCliente->>Gateway: POST /api/compras/reservas (asiento: 15)
    Gateway->>Compras: POST /api/compras/reservas
    Compras->>DB: INSERT BloqueoAsiento (asiento: 15 para OtroCliente)
    DB-->>Compras: OK (Reserva exitosa para OtroCliente)
    Compras-->>OtroCliente: 201 Created

    %% 4. Intento tardío del cliente original
    Cliente->>Gateway: POST /api/compras/ordenes/{id}/pagar
    Gateway->>Compras: POST /api/compras/ordenes/{id}/pagar
    Compras->>DB: SELECT Orden WHERE id = {id}
    DB-->>Compras: Orden en estado 'EXPIRADA' (o fechaExpiracion < NOW())
    
    Note over Compras: La reserva venció: NO se invoca a la pasarela de pagos
    Compras-->>Gateway: 409 Conflict / 410 Gone ("La reserva temporal ha expirado")
    Gateway-->>Cliente: 409 Conflict ("Tiempo agotado. La reserva fue liberada")
```

4. Autenticación y filtro JWT

Escenario Flujo 1: El cliente envía sus credenciales al punto de entrada unico(API Gateway). El servicio verifica la identidad, se construye el JWT firmado con la clave secreta, incrustando en el payload el usuario, la fecha de expiracion y la lista de permisos o roles. Se responde al cliente con el token en formato Bearer.
Escenario Flujo 2: Todas las invocaciones a servicios REST protegidos adjuntan la cabecera Authorization: Bearer <token>. La clase `JWTAuthorizationFilter` intercepta la solicitud HTTP entrante antes de que llegue a los controladores. El filtro invoca a `Jwts.parser()` utilizando la clave secreta compartida para descifrar la firma y comprobar que el token no haya vencido ni haya sido alterado externamente. Si el token es válido, extrae los roles y configura el objeto `UsernamePasswordAuthenticationToken` dentro del `SecurityContextHolder` de Spring. Luego llama a `filterChain.doFilter(...)` para permitir el paso hacia el `RestController`. Si el token es inválido o expirado, se captura la excepción (`ExpiredJwtException`, `MalformedJwtException`), se limpia el contexto de seguridad y se devuelve de inmediato un estado HTTP `401 Unauthorized` o `403 Forbidden`.

```mermaid
sequenceDiagram 
    autonumber 
    actor Cliente as Cliente / Frontend 
    participant Gateway as API Gateway (:8080) 
    participant AuthCtrl as AuthController / Service 
    participant Filter as JWTAuthorizationFilter 
    participant SecContext as SecurityContextHolder 
    participant RestCtrl as Microservicio (RestController) 
    participant DB as Base de Datos 
    

    %% FLUJO 1: AUTENTICACIÓN (LOGIN) 

    rect rgb(235, 245, 255) 
        note over Cliente, DB: Flujo 1: Autenticación y Emisión de Token (POST /api/auth/login) 
        Cliente->>Gateway: POST /api/auth/login {username, password} 
        Gateway->>AuthCtrl: Rutea solicitud de login 
        AuthCtrl->>DB: Consultar usuario y clave encriptada (con salt) 
        DB-->>AuthCtrl: Devuelve registro de usuario y credenciales 
        alt Credenciales Válidas 
            AuthCtrl->>AuthCtrl: Validar hash de contraseña (PasswordUtils / BCrypt) 
            AuthCtrl->>AuthCtrl: Generar JWT (Jwts.builder) con subject, roles y expiración 
            AuthCtrl-->>Gateway: 200 OK { token: "Bearer eyJhbGci..." } 
            Gateway-->>Cliente: 200 OK { token: "Bearer eyJhbGci..." } 
        else Credenciales Inválidas 
            AuthCtrl-->>Gateway: 401 Unauthorized { error: "Credenciales incorrectas" } 
            Gateway-->>Cliente: 401 Unauthorized 
        end 
    end
    
  
    %% FLUJO 2: AUTORIZACIÓN Y PETICIÓN PROTEGIDA 
  
    rect rgb(240, 250, 240) 
        note over Cliente, RestCtrl: Flujo 2: Petición Protegida con Filtro JWTAuthorizationFilter 
        Cliente->>Gateway: GET/POST /api/recurso-protegido<br/>Header Authorization: Bearer <token>
        Gateway->>Filter: Rutea petición hacia el microservicio de destino 
        Filter->>Filter: Verificar presencia de encabezado "Authorization" 
        alt Token Ausente, Expirado o Firma Inválida
            Filter->>SecContext: SecurityContextHolder.clearContext() 
            Filter-->>Gateway: 401 Unauthorized / 403 Forbidden 
            Gateway-->>Cliente: 401 / 403 HTTP Error Response
        else Token Válido y Firmado Correctamente
            Filter->>Filter: Desencriptar y parsear claims (Jwts.parser con secretKey) 
            Filter->>Filter: Extraer subject y permisos ("authorities") 
            Filter->>SecContext: setAuthentication(UsernamePasswordAuthenticationToken) 
            Filter->>RestCtrl: filterChain.doFilter(request, response) 
            RestCtrl->>DB: Procesar lógica de negocio del endpoint 
            DB-->>RestCtrl: Devuelve resultado 
            RestCtrl-->>Gateway: 200 OK / 201 Created (JSON Response) 
            Gateway-->>Cliente: 200 OK / 201 Created (JSON Response)
        end 
    end
```
