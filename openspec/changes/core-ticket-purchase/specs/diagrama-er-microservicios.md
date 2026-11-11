# TicketYa — diagramas entidad-relación por microservicio

Diagramas derivados de las entidades/modelos y configuración del repositorio. Las líneas discontinuas representan referencias entre servicios por identificadores; no son claves foráneas ni implican acceso directo a otra base. Las relaciones sólidas representan vínculos dentro de la persistencia del servicio.

## Persistencia por servicio

| Servicio | Base / almacenamiento | Entidades persistidas |
| --- | --- | --- |
| `eventos-service` | MongoDB `db_eventos` | `Evento`, `Lugar`; sectores, precios, filas y asientos son documentos embebidos/listas. |
| `compras-service` | PostgreSQL `db_compras` | `OrdenCompra`, `DetalleReserva`, `CupoSector`, `BloqueoAsiento`. |
| `entradas-service` | PostgreSQL `db_entradas` | `Entrada`, `SolicitudDevolucion`. |
| `pasarela-mock-service` | Sin persistencia configurada | No hay entidades persistentes. Las respuestas de pago/reembolso son simuladas y no se almacenan. |
| `api-gateway` | Sin persistencia | Enruta solicitudes; no contiene entidades de negocio. |

## 1. eventos-service — MongoDB `db_eventos`

```mermaid
erDiagram
    LUGAR {
        string id PK "Mongo _id"
        string nombre
        string direccion
        string ciudad
        object ubicacion "latitud, longitud; embebido"
        array sectores "Sector embebido: sectorId, nombre, tipo, capacidadMaxima, filas/asientos"
    }

    EVENTO {
        string id PK "Mongo _id"
        string nombre
        string descripcion
        string tipoEvento
        string organizadorId "referencia externa, sin entidad Usuario"
        array participantes "IDs; sin entidad Participante"
        datetime fechaHora
        string lugarId FK "referencia lógica a LUGAR.id"
        string ciudad
        string estado
        array preciosSectores "idSector + precio; embebido"
        array sectores "resumen de sectores embebido"
    }

    LUGAR o|--o{ EVENTO : "lugarId (referencia lógica)"
```

**Claves y relaciones.** `id` es el identificador Mongo (`_id`) de cada documento. `Evento.lugarId` identifica un documento `Lugar`, pero no se declara una referencia Mongo ni una clave foránea. Un lugar puede relacionarse con cero o varios eventos; cada evento contiene un `lugarId` escalar. Sectores, precios, ubicación, filas y asientos no son colecciones independientes en este modelo: están embebidos.

## 2. compras-service — PostgreSQL `db_compras`

```mermaid
erDiagram
    ORDEN_COMPRA {
        UUID id PK
        string clienteId "ID de cliente; entidad externa"
        string eventoId "ID de evento; referencia lógica a eventos-service"
        datetime fechaCreacion
        datetime fechaExpiracionReserva
        string estado
        decimal montoTotal
        string metodoPago
        string transaccionPagoId "ID devuelto por pasarela; no hay FK"
        string idempotencyKey UK "única si no es null"
    }

    DETALLE_RESERVA {
        UUID id PK
        UUID orden_id FK "JoinColumn; relación ORM con ORDEN_COMPRA"
        string sectorId "ID de sector; referencia lógica"
        string fila "nullable para capacidad general"
        int asiento "nullable para capacidad general"
        decimal precioUnitario
    }

    CUPO_SECTOR {
        UUID id PK
        string evento_id "ID externo de evento"
        string sector_id "ID externo de sector"
        int capacidad_total
        int cupos_disponibles
        string evento_sector UK "restricción única compuesta (evento_id, sector_id)"
    }

    BLOQUEO_ASIENTO {
        UUID id PK
        string eventoId "ID externo de evento"
        string sectorId "ID externo de sector"
        string clave_asiento UK "única; clave compuesta serializada"
        UUID ordenId "referencia lógica a ORDEN_COMPRA.id"
        string estado
        datetime expiraEn
    }

    ORDEN_COMPRA ||--o{ DETALLE_RESERVA : "orden_id"
    ORDEN_COMPRA ||--o{ BLOQUEO_ASIENTO : "ordenId lógico"
```

**Claves y relaciones.** Las PK son UUID. `DetalleReserva.orden_id` es la relación ORM declarada mediante `@OneToMany`/`@JoinColumn`. `BloqueoAsiento.ordenId` es un UUID que vincula lógicamente el bloqueo con su orden, sin relación JPA declarada. `CupoSector` tiene unicidad compuesta `(evento_id, sector_id)`; `BloqueoAsiento.clave_asiento` e `OrdenCompra.idempotencyKey` tienen unicidad. `eventoId`, `sectorId`, `clienteId` y `transaccionPagoId` son valores escalares; no se declaran FK a otros servicios.

## 3. entradas-service — PostgreSQL `db_entradas`

```mermaid
erDiagram
    ENTRADA {
        UUID id PK
        string ordenCompraId "referencia lógica a compras-service.OrdenCompra.id"
        string clienteId "ID de cliente; entidad externa"
        string eventoId "referencia lógica a eventos-service.Evento.id"
        string sectorNombre "copia del nombre; no sectorId"
        string fila "nullable"
        int asiento "nullable"
        string codigoQrHash UK
        string estado
        datetime fechaEmision
    }

    SOLICITUD_DEVOLUCION {
        UUID id PK
        UUID entradaId "referencia lógica a ENTRADA.id"
        string clienteId "ID de cliente; entidad externa"
        string motivo
        string estado
        datetime fechaSolicitud
        datetime fechaResolucion "nullable"
        string respuestaAdmin
    }

    ENTRADA ||--o{ SOLICITUD_DEVOLUCION : "entradaId lógico"
```

**Claves y relaciones.** Ambas entidades usan PK UUID. `Entrada.codigoQrHash` es único. `SolicitudDevolucion.entradaId` y `clienteId` no tienen una asociación JPA/FK declarada; el servicio busca la entrada por su UUID y aplica reglas de devolución. Una entrada puede tener cero o varias solicitudes históricas; el servicio impide más de una pendiente para la misma entrada. `ordenCompraId` y `eventoId` refieren a IDs de otras bases, sin FK.

## 4. pasarela-mock-service y api-gateway

No hay ERD persistente: ninguno configura una base de datos ni declara entidades persistentes. La pasarela crea IDs de transacción/reembolso para las respuestas simuladas, pero no guarda pagos. `compras-service` conserva `transaccionPagoId` en `OrdenCompra`; no se ha encontrado almacenamiento de la otra parte en pasarela.

## Relaciones entre servicios

```mermaid
flowchart LR
    subgraph EV["eventos-service · db_eventos"]
        E["Evento<br/>id, lugarId"]
        L["Lugar<br/>id, sectores embebidos"]
        L -. "lugarId" .-> E
    end
    subgraph CO["compras-service · db_compras"]
        O["OrdenCompra<br/>id, eventoId, clienteId"]
        D["DetalleReserva<br/>sectorId"]
        B["BloqueoAsiento<br/>eventoId, sectorId, ordenId"]
        C["CupoSector<br/>evento_id, sector_id"]
        O --> D
        O -. "ordenId" .-> B
    end
    subgraph EN["entradas-service · db_entradas"]
        T["Entrada<br/>id, ordenCompraId, eventoId, clienteId"]
        R["SolicitudDevolucion<br/>entradaId, clienteId"]
        T -. "entradaId" .-> R
    end
    subgraph PA["pasarela-mock-service · sin BD"]
        P["Pago/reembolso simulado<br/>IDs solo en respuesta"]
    end
    E -. "eventoId" .-> O
    E -. "eventoId" .-> T
    O -. "ordenCompraId" .-> T
    P -. "transaccionPagoId de respuesta; compras lo guarda" .-> O
```

Las flechas discontinuas entre servicios representan IDs intercambiados por API. El flujo documentado en código incluye consultas de compras a eventos y pasarela, emisión de entradas desde compras, y consultas de eventos desde entradas. Las flechas punteadas del dibujo muestran la relación semántica de datos, no necesariamente una llamada unidireccional. El modelo de entradas usa `sectorNombre` en vez de `sectorId`, por lo que no mantiene una clave directa al sector del catálogo.

## Entidades o datos solapados entre bases

| Dato/entidad conceptual | Dónde aparece | Tipo de solapamiento |
| --- | --- | --- |
| Evento | `db_eventos.Evento.id`; `db_compras.OrdenCompra.eventoId`, `CupoSector.evento_id`, `BloqueoAsiento.eventoId`; `db_entradas.Entrada.eventoId` | El evento es dueño del catálogo en Eventos. Compras y Entradas guardan referencias/copiados de su ID, no una entidad Evento local ni una FK entre bases. |
| Sector | Embebido en `Lugar.sectores`; resumido también en `Evento.sectores` y `Evento.preciosSectores`; IDs en `DetalleReserva`, `CupoSector`, `BloqueoAsiento`; texto `Entrada.sectorNombre` | Hay solapamiento real de información: el resumen/ precio en `Evento` duplica parcialmente el sector definido en `Lugar`; compras guarda IDs para reserva/cupo y entradas copia el nombre. No hay tabla/colección Sector independiente. |
| Orden de compra | `db_compras.OrdenCompra.id`; `db_entradas.Entrada.ordenCompraId`; además `BloqueoAsiento.ordenId` en Compras | Una sola entidad propietaria en Compras. Entradas conserva el identificador asociado, sin duplicar los datos de la orden ni declarar FK. |
| Cliente | `OrdenCompra.clienteId`; `Entrada.clienteId`; `SolicitudDevolucion.clienteId` | Identificador repetido entre bases/entidades. No existe servicio o entidad Cliente en el repositorio inspeccionado. |
| Transacción de pago | `OrdenCompra.transaccionPagoId`; la pasarela genera `transaccionId` como respuesta | El ID queda persistido en Compras; la pasarela mock no mantiene una entidad/transacción persistida. |

### Lectura recomendada

Hay tres dominios con datos persistidos (`eventos`, `compras`, `entradas`) y dos servicios sin persistencia (`pasarela-mock`, `api-gateway`). No existe una base compartida desde el punto de vista de los servicios: aunque compras y entradas usan el mismo servidor PostgreSQL, están configurados en bases separadas (`db_compras` y `db_entradas`). Los IDs cruzados deben tratarse como referencias de API y consistencia eventual, no como integridad referencial garantizada por SQL.
