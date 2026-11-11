## 1. Diagrama de Módulos (Componentes del Flujo de Compra)

Este diagrama representa cómo se interconectan los microservicios y bases de datos durante el ciclo de vida de una reserva y compra

mermaid
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

## 2. Diagrama de Secuencia: Compra con Pago Rechazado

Escenario: El cliente bloquea un asiento correctamente, pero la pasarela de pagos rechaza la transacción (por fondos insuficientes, tarjeta inválida, etc.)  El sistema debe actualizar la orden a RECHAZADA y liberar el asiento de inmediato para no retenerlo innecesariamente

mermaid
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


---

## 3. Diagrama de Secuencia: Reserva Vencida (Timeout de los 10 minutos)

*Escenario:* El cliente genera la reserva temporal, pero abandona la pantalla o tarda más del tiempo límite (ej. 10 minutos) sin completar el pago 
Aquí intervienen dos mecanismos concurrentes 
1.El Job en segundo plano @Scheduled que barre periódicamente y libera los bloqueos expirados.
2.La validación defensiva al intentar pagar tardíamente*, que rechaza la operación informando que la reserva expiró

mermaid
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


