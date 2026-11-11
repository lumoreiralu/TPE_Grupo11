# TicketYa

TicketYa es una plataforma de venta de entradas para eventos. El sistema está compuesto por
cinco microservicios Spring Boot, dos bases de datos y una interfaz web en React.

El flujo principal permite buscar eventos publicados, elegir sector y asientos, reservar
temporalmente las entradas, simular el pago, emitir tickets digitales con código QR y consultar
las entradas por orden o por cliente.

## Arquitectura

```text
Navegador
   |
   v
frontend (React + Vite, :5173)
   |
   v
api-gateway (:8080)
   |-- /api/eventos/**  -> eventos-service (:8081)   -> MongoDB (:27017)
   |-- /api/lugares/**  -> eventos-service (:8081)   -> MongoDB (:27017)
   |-- /api/compras/**  -> compras-service (:8082)   -> PostgreSQL (:5433)
   |-- /api/entradas/** -> entradas-service (:8083)  -> PostgreSQL (:5433)
   `-- /api/pagos/**    -> pasarela-mock-service (:8084)
```

| Servicio | Responsabilidad | Puerto | Persistencia |
| --- | --- | --- | --- |
| `api-gateway` | Punto de entrada único y ruteo | `8080` | — |
| `eventos-service` | Lugares, sectores, eventos y búsqueda | `8081` | MongoDB `db_eventos` |
| `compras-service` | Reservas, bloqueos, pagos y órdenes | `8082` | PostgreSQL `db_compras` |
| `entradas-service` | Emisión de entradas y códigos QR | `8083` | PostgreSQL `db_entradas` |
| `pasarela-mock-service` | Simulación de cobros y reembolsos | `8084` | — |
| `frontend` | Interfaz web del cliente | `5173` | `localStorage` |

## Estructura del repositorio

```text
TicketYa/
|-- api-gateway/
|-- eventos-service/
|-- compras-service/
|   `-- compras-service/
|-- entradas-service/
|-- pasarela-mock-service/
|-- frontend/
|-- postman/
|   `-- TicketYa.postman_collection.json
|-- init-scripts/
|   `-- init.sql
|-- docs/adr/
|-- openspec/
`-- docker-compose.yml
```

## Requisitos

- Java 17.
- Node.js 20 o superior.
- Docker Desktop con Docker Compose (recomendado para MongoDB y PostgreSQL).
- Opcional: Postman, Insomnia o Bruno para ejecutar la colección de requests.

Cada servicio incluye el Maven Wrapper, por lo que no hace falta instalar Maven por separado.

## Inicio rápido

### 1. Levantar las bases de datos

Desde la raíz del repositorio:

```bash
docker compose up -d
docker compose ps
```

Esto crea:

- MongoDB en `localhost:27017`, base `db_eventos`.
- PostgreSQL en `localhost:5433`, usuario `postgres`, contraseña `postgrespassword`.
- Bases `db_compras` y `db_entradas` mediante `init-scripts/init.sql`.

Si usás una versión anterior de Docker, reemplazá `docker compose` por `docker-compose`.

Para detener las bases sin borrar datos:

```bash
docker compose down
```

Para reiniciar desde cero, borrando los volúmenes:

```bash
docker compose down -v
```

> `docker compose down -v` elimina los datos de MongoDB y PostgreSQL.

### 2. Levantar los microservicios

Cada servicio debe ejecutarse en una terminal separada. En Windows PowerShell usá
`.\mvnw.cmd`; en Linux/macOS o Git Bash usá `./mvnw`.

Orden recomendado:

**Terminal 1 — eventos-service**

```powershell
cd eventos-service
.\mvnw.cmd spring-boot:run
```

```bash
cd eventos-service
./mvnw spring-boot:run
```

**Terminal 2 — entradas-service**

```powershell
cd entradas-service
.\mvnw.cmd spring-boot:run
```

```bash
cd entradas-service
./mvnw spring-boot:run
```

**Terminal 3 — pasarela-mock-service**

```powershell
cd pasarela-mock-service
.\mvnw.cmd spring-boot:run
```

```bash
cd pasarela-mock-service
./mvnw spring-boot:run
```

**Terminal 4 — compras-service**

```powershell
cd compras-service\compras-service
.\mvnw.cmd spring-boot:run
```

```bash
cd compras-service/compras-service
./mvnw spring-boot:run
```

**Terminal 5 — api-gateway**

```powershell
cd api-gateway
.\mvnw.cmd spring-boot:run
```

```bash
cd api-gateway
./mvnw spring-boot:run
```

El gateway debe iniciarse después de los servicios que enruta.

### 3. Levantar el frontend

```bash
cd frontend
npm install
npm run dev
```

Abrí `http://localhost:5173`.

Vite hace proxy de `/api` hacia `http://localhost:8080`, así que en desarrollo no hace falta
configurar CORS. El frontend debe servirse con Vite; **no abras `frontend/index.html` directamente**
con el navegador ni con Live Server.

### 4. Verificar que el gateway responde

```bash
curl http://localhost:8080/api/eventos/publicados
```

En Windows PowerShell:

```powershell
curl.exe http://localhost:8080/api/eventos/publicados
```

Si todavía no cargaste datos, la respuesta será `[]`. Usá la colección de Postman o la sección
[Datos de prueba](#datos-de-prueba) para crear un evento publicado.

## Configuración

### Backend

La configuración principal está en los `application.properties` de cada servicio:

| Servicio | Archivo | Clave | Valor por defecto |
| --- | --- | --- | --- |
| eventos | `eventos-service/src/main/resources/application.properties` | `spring.data.mongodb.uri` | `mongodb://localhost:27017/db_eventos` |
| compras | `compras-service/compras-service/src/main/resources/application.properties` | `spring.datasource.url` | `jdbc:postgresql://localhost:5433/db_compras` |
| compras | ídem | `spring.datasource.username` | `postgres` |
| compras | ídem | `spring.datasource.password` | `postgrespassword` |
| compras | ídem | `eventos.service.url` | `http://localhost:8081` |
| compras | ídem | `pasarela.service.url` | `http://localhost:8084` |
| compras | ídem | `entradas.service.url` | `http://localhost:8083` |
| compras | ídem | `compras.reserva.minutos` | `10` |
| compras | ídem | `compras.limite-entradas-por-cliente` | `4` |
| entradas | `entradas-service/src/main/resources/application.properties` | `spring.datasource.url` | `jdbc:postgresql://localhost:5433/db_entradas` |
| gateway | `api-gateway/src/main/resources/application.yml` | `server.port` | `8080` |

Si cambiás los puertos o las URLs de las bases, actualizá también las rutas del gateway y las
URLs entre servicios.

### Frontend

```bash
cd frontend
cp .env.example .env.local
```

Variables disponibles:

| Variable | Default | Descripción |
| --- | --- | --- |
| `VITE_GATEWAY_URL` | `http://localhost:8080` | Destino del proxy de Vite para `/api`. |
| `VITE_API_BASE_URL` | vacío | Si se define, el navegador llama directo a esa URL. Requiere CORS en el gateway. |
| `VITE_MAX_ENTRADAS` | `4` | Máximo de entradas por compra en la UI. Debe coincidir con el backend. |

## Datos de prueba

La colección de Postman crea estos datos automáticamente y guarda los IDs en variables. Si preferís
hacerlo manualmente, usá los mismos valores:

| Dato | Valor |
| --- | --- |
| Cliente | `CLI-1234` |
| Organizador | `ORG-101` |
| Lugar | `Estadio TicketYa`, `Av. Principal 123`, `Montevideo` |
| Coordenadas | latitud `-34.9011`, longitud `-56.1645` |
| Sector numerado | `SEC-PLATEA-A` (`Platea Preferencial`, filas `1` y `2`, asientos `1` a `8`) |
| Sector general | `SEC-CAMPO` (`Campo General`, capacidad `5000`) |
| Evento | `Recital TicketYa Tour`, tipo `RECITAL`, fecha `2027-12-15T21:00:00` |
| Participantes | `Artista A`, `Banda B` |
| Precios | `SEC-PLATEA-A`: `30000`, `SEC-CAMPO`: `15000` |
| Asientos usados en el flujo principal | fila `1`, asientos `1` y `2` |

El frontend solo muestra eventos en estado `PUBLICADO`. Crear un evento lo deja en `BORRADOR`
hasta ejecutar `PUT /api/eventos/{id}/publicar`.

### Opción A: colección de Postman (recomendada)

Importá `postman/TicketYa.postman_collection.json` en Postman. La colección incluye las variables
necesarias y va guardando automáticamente `lugarId`, `eventoId`, `ordenId`, `transaccionPagoId`,
`qrHash` y demás IDs a medida que ejecutás las requests.

1. Abrí Postman y elegí **Import**.
2. Seleccioná el archivo `postman/TicketYa.postman_collection.json`.
3. Revisá que `baseUrl` sea `http://localhost:8080`.
4. Ejecutá las carpetas en orden:
   - `00 - Catálogo y datos de prueba`
   - `01 - Flujo de compra`
   - `02 - Entradas`
   - `03 - Pasarela mock`
   - `04 - Casos de error`
   - `05 - Endpoint interno (opcional)`
5. También podés usar **Collection Runner** para ejecutar toda la colección de arriba hacia abajo.

Si ejecutás una request aislada, primero ejecutá las requests de catálogo para que existan
`lugarId`, `eventoId` y `ordenId`.

### Opción B: curl desde Linux/macOS/Git Bash

Necesitás `jq`. El siguiente flujo crea el lugar, el evento, lo publica, reserva dos asientos,
confirma el pago y consulta las entradas:

```bash
GATEWAY=http://localhost:8080

LUGAR_ID=$(curl -s -X POST "$GATEWAY/api/lugares" \
  -H 'Content-Type: application/json' \
  -d '{
    "nombre": "Estadio TicketYa",
    "direccion": "Av. Principal 123",
    "ciudad": "Montevideo",
    "ubicacion": { "latitud": -34.9011, "longitud": -56.1645 },
    "sectores": [
      {
        "sectorId": "SEC-PLATEA-A",
        "nombre": "Platea Preferencial",
        "tipo": "NUMERADO",
        "filas": [
          { "fila": "1", "asientos": [1, 2, 3, 4, 5, 6, 7, 8] },
          { "fila": "2", "asientos": [1, 2, 3, 4, 5, 6, 7, 8] }
        ]
      },
      {
        "sectorId": "SEC-CAMPO",
        "nombre": "Campo General",
        "tipo": "CAPACIDAD_GENERAL",
        "capacidadMaxima": 5000
      }
    ]
  }' | jq -r .id)

EVENTO_ID=$(curl -s -X POST "$GATEWAY/api/eventos" \
  -H 'Content-Type: application/json' \
  -d "{
    \"nombre\": \"Recital TicketYa Tour\",
    \"descripcion\": \"Una noche de musica en vivo.\",
    \"tipoEvento\": \"RECITAL\",
    \"organizadorId\": \"ORG-101\",
    \"participantes\": [\"Artista A\", \"Banda B\"],
    \"fechaHora\": \"2027-12-15T21:00:00\",
    \"lugarId\": \"$LUGAR_ID\",
    \"ciudad\": \"Montevideo\",
    \"preciosSectores\": [
      { \"idSector\": \"SEC-PLATEA-A\", \"precio\": 30000 },
      { \"idSector\": \"SEC-CAMPO\", \"precio\": 15000 }
    ]
  }" | jq -r .id)

curl -s -X PUT "$GATEWAY/api/eventos/$EVENTO_ID/publicar"

ORDEN_ID=$(curl -s -X POST "$GATEWAY/api/compras/reservar" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-001' \
  -d "{
    \"clienteId\": \"CLI-1234\",
    \"eventoId\": \"$EVENTO_ID\",
    \"items\": [
      { \"sectorId\": \"SEC-PLATEA-A\", \"fila\": \"1\", \"asiento\": 1 },
      { \"sectorId\": \"SEC-PLATEA-A\", \"fila\": \"1\", \"asiento\": 2 }
    ]
  }" | jq -r .ordenId)

curl -s -X POST "$GATEWAY/api/compras/$ORDEN_ID/confirmar-pago" \
  -H 'Content-Type: application/json' \
  -d '{"metodoPago":"TARJETA"}'

curl -s "$GATEWAY/api/entradas/orden/$ORDEN_ID"
curl -s "$GATEWAY/api/entradas/cliente/CLI-1234"
```

En Windows PowerShell podés usar `Invoke-RestMethod` o, más simple, ejecutar la colección de
Postman. Si usás `curl.exe`, tené en cuenta que las comillas y el escape de JSON cambian respecto
de Bash.

## Endpoints

Todas las rutas se consumen a través del gateway en `http://localhost:8080`.

### Lugares

| Método | Ruta | Éxito | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/lugares` | `201` | Crea un lugar con sus sectores. |
| `GET` | `/api/lugares` | `200` | Lista todos los lugares. |
| `GET` | `/api/lugares/{id}` | `200` / `404` | Obtiene un lugar por ID. |

### Eventos

| Método | Ruta | Éxito | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/eventos` | `201` | Crea un evento en estado `BORRADOR`. |
| `GET` | `/api/eventos` | `200` | Lista todos los eventos, incluidos los borradores. |
| `GET` | `/api/eventos/{id}` | `200` / `404` | Obtiene un evento por ID. |
| `GET` | `/api/eventos/publicados` | `200` | Lista eventos en estado `PUBLICADO`. |
| `GET` | `/api/eventos/buscar` | `200` | Busca eventos publicados por filtros. |
| `PUT` | `/api/eventos/{id}/publicar` | `200` / `404` / `422` | Publica el evento si tiene precios válidos. |

Parámetros de búsqueda: `tipo`, `organizadorId`, `artista`, `ciudad`, `fechaDesde`, `fechaHasta`.
Las fechas usan formato `YYYY-MM-DD`. Los tipos válidos son `RECITAL`, `TEATRO`, `DEPORTE` y
`CONFERENCIA`.

Ejemplo:

```text
GET /api/eventos/buscar?tipo=RECITAL&ciudad=Montevideo&artista=Artista&fechaDesde=2027-12-01&fechaHasta=2027-12-31
```

### Compras

| Método | Ruta | Éxito | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/compras/reservar` | `201` | Reserva asientos durante 10 minutos. |
| `POST` | `/api/compras/{id}/confirmar-pago` | `200` | Procesa el pago y emite las entradas. |
| `GET` | `/api/compras/{id}` | `200` / `404` | Consulta el estado de una orden. |

La reserva acepta el header opcional `Idempotency-Key`. Si se repite con la misma clave, devuelve
la misma orden en lugar de crear otra.

Ejemplo de reserva:

```http
POST /api/compras/reservar
Content-Type: application/json
Idempotency-Key: demo-001

{
  "clienteId": "CLI-1234",
  "eventoId": "ID_DEL_EVENTO",
  "items": [
    { "sectorId": "SEC-PLATEA-A", "fila": "1", "asiento": 1 },
    { "sectorId": "SEC-PLATEA-A", "fila": "1", "asiento": 2 }
  ]
}
```

Respuesta:

```json
{
  "ordenId": "uuid-de-la-orden",
  "estado": "RESERVADA_TEMPORAL",
  "expiraEn": "2027-12-15T21:10:00",
  "montoTotal": 60000.00,
  "segundosRestantes": 600
}
```

Ejemplo de confirmación:

```http
POST /api/compras/{ordenId}/confirmar-pago
Content-Type: application/json

{
  "metodoPago": "TARJETA"
}
```

La respuesta incluye `estado: "COMPLETADA"`, `transaccionPagoId` y el arreglo `entradas` con los
tickets emitidos.

### Entradas

| Método | Ruta | Éxito | Descripción |
| --- | --- | --- | --- |
| `GET` | `/api/entradas/orden/{ordenCompraId}` | `200` | Lista las entradas de una orden. |
| `GET` | `/api/entradas/cliente/{clienteId}` | `200` | Lista las entradas de un cliente. |
| `POST` | `/api/entradas/emitir` | `201` | Emisión interna; normalmente la invoca compras. |

### Pagos mock

| Método | Ruta | Éxito | Descripción |
| --- | --- | --- | --- |
| `POST` | `/api/pagos/procesar` | `200` | Simula un pago. |
| `POST` | `/api/pagos/reembolsar` | `200` | Simula un reembolso. |

Comportamiento de la pasarela según `metodoPago`:

| Valor contiene | Resultado |
| --- | --- |
| `TARJETA` u otro valor normal | `APROBADO` |
| `RECHAZ`, `FONDOS` o `DENEGA` | `RECHAZADO` |
| `INVALIDA` o `VENCIDA` | `RECHAZADO` |
| `ERROR` o `TIMEOUT` | Error HTTP `500` |

La pasarela también rechaza montos nulos, cero o negativos.

## Flujo completo de compra

1. Crear un lugar con sectores numerados y, opcionalmente, un sector de capacidad general.
2. Crear un evento con `preciosSectores` usando los IDs de sector del lugar.
3. Publicar el evento con `PUT /api/eventos/{id}/publicar`.
4. Buscar el evento con `GET /api/eventos/buscar` o `GET /api/eventos/publicados`.
5. Reservar los asientos con `POST /api/compras/reservar` y guardar `ordenId`.
6. Confirmar el pago con `POST /api/compras/{ordenId}/confirmar-pago`.
7. Consultar la orden con `GET /api/compras/{ordenId}`.
8. Ver las entradas con `GET /api/entradas/orden/{ordenId}` o
   `GET /api/entradas/cliente/{clienteId}`.

La reserva vence a los 10 minutos. Si el pago no se confirma antes, los asientos se liberan y la
orden pasa a `EXPIRADA`. Un pago rechazado libera los asientos y deja la orden en `CANCELADA`.

## Colección de Postman

Archivo:

```text
postman/TicketYa.postman_collection.json
```

La colección es autoejecutable: crea los datos de prueba, captura los IDs de las respuestas y
verifica códigos HTTP y campos importantes. No requiere copiar IDs manualmente.

Carpetas:

| Carpeta | Contenido |
| --- | --- |
| `00 - Catálogo y datos de prueba` | Crear lugar, crear evento, publicar, buscar y listar. |
| `01 - Flujo de compra` | Reserva, reintento idempotente, consulta y confirmación de pago. |
| `02 - Entradas` | Entradas por orden y por cliente. |
| `03 - Pasarela mock` | Pago aprobado, rechazado, error técnico y reembolso. |
| `04 - Casos de error` | Asiento duplicado, límite, evento no publicado, pago rechazado y 404. |
| `05 - Endpoint interno (opcional)` | Emisión directa de entradas. |

Variables principales: `baseUrl`, `clienteId`, `organizadorId`, `eventoFecha`, `sectorNumerado`,
`sectorGeneral`, `fila`, `lugarId`, `eventoId`, `ordenId`, `transaccionPagoId` y
`qrHash`.

La request de reserva genera una `Idempotency-Key` nueva y la request de reintento usa la misma
clave para comprobar que no se crea una segunda orden.

## Tests

### Backend

Ejecutá los tests desde el directorio de cada servicio:

```powershell
cd eventos-service
.\mvnw.cmd test
```

```bash
cd eventos-service
./mvnw test
```

Repetí con:

```powershell
cd compras-service\compras-service
.\mvnw.cmd test

cd entradas-service
.\mvnw.cmd test

cd pasarela-mock-service
.\mvnw.cmd test

cd api-gateway
.\mvnw.cmd test
```

En Linux/macOS usá `./mvnw test` y las rutas con `/`.

Los tests de compras y entradas usan H2 en memoria. Los clientes HTTP de eventos, pasarela y
entradas se mockean en los tests de compras.

### Frontend

```bash
cd frontend
npm run build
npm run lint
```

## Troubleshooting

### `ECONNREFUSED 127.0.0.1:8080`

El gateway no está corriendo o no está escuchando en `8080`. Verificá que `api-gateway` esté
iniciado y que `http://localhost:8080/api/eventos/publicados` responda.

### La página queda en blanco

No abras `frontend/index.html` con doble clic ni con Live Server. Ejecutá `npm run dev` y entrá a
`http://localhost:5173`. Abrir el archivo directamente produce errores del tipo
`file:///C:/src/main.tsx` y `/C:/favicon.svg`.

### Error de autenticación o puerto con PostgreSQL

El proyecto espera PostgreSQL en `localhost:5433` con usuario `postgres` y contraseña
`postgrespassword`. Si tenés un PostgreSQL local en otro puerto, como `6432`, no lo uses salvo que
cambies `spring.datasource.url` en compras y entradas. La opción recomendada es usar el PostgreSQL
de Docker Compose.

### Docker no está corriendo

Abrí Docker Desktop y esperá a que el motor inicie. Luego ejecutá de nuevo:

```bash
docker compose up -d
docker compose ps
```

### WSL no llega al gateway de Windows

Si el frontend corre en WSL y el backend en Windows, `localhost` puede no ser el mismo. Opciones:

- Ejecutar frontend y backend en el mismo sistema operativo.
- Definir el host de Windows en `frontend/.env.local`:

  ```text
  VITE_GATEWAY_URL=http://172.29.0.1:8080
  ```

  La IP puede cambiar; verificala con `ip route show default` dentro de WSL.
- Habilitar el modo de red espejado de WSL2 y reiniciar WSL.
- Permitir el puerto `8080` en Windows Firewall si el firewall bloquea la conexión.

### El evento no aparece en el frontend

El evento debe estar en estado `PUBLICADO` y tener al menos un precio por sector mayor a cero.
Ejecutá `PUT /api/eventos/{id}/publicar` y revisá la respuesta.

### `409` al reservar un asiento

El asiento ya está reservado o vendido. Probá con otro asiento o esperá a que venza la reserva de
10 minutos. En la colección de Postman, cada ejecución completa crea un evento nuevo, por lo que
los asientos vuelven a estar libres.

### `402` al confirmar el pago

La pasarela mock rechazó el pago. Usá `"metodoPago": "TARJETA"` para el camino aprobado. Los
métodos que contienen `RECHAZ`, `FONDOS`, `DENEGA`, `INVALIDA` o `VENCIDA` se rechazan a propósito.

### “Mis entradas” no muestra nada

El frontend usa el `clienteId` guardado en `localStorage`. Si reservaste con `CLI-1234` desde
Postman, seleccioná ese mismo cliente en la UI o consultá
`GET /api/entradas/cliente/CLI-1234`.

## Limitaciones conocidas

- No hay autenticación ni autorización: el `clienteId` es la identidad.
- La reserva desde `compras-service` solo soporta asientos numerados. Los sectores de capacidad
  general se muestran en el catálogo, pero no se reservan desde la UI.
- La pasarela es simulada; no se cobra dinero real.
- La emisión de entradas es idempotente por `ordenCompraId`, pero no existe todavía un endpoint de
  validación o check-in de QR.
- No hay endpoints para cancelar una compra o solicitar reembolso desde el frontend. El reembolso
  solo se puede probar contra la pasarela mock.
- El proyecto no incluye CORS habilitado para llamadas directas del navegador al gateway; en
  desarrollo se usa el proxy de Vite.

## Documentación adicional

- Decisiones de arquitectura: `docs/adr/`.
- Cambio funcional y checklist: `openspec/changes/core-ticket-purchase/`.
- Detalle del frontend: `frontend/README.md`.
- Colección de requests: `postman/TicketYa.postman_collection.json`.
