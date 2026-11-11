# TicketYa Frontend

Interfaz web en **React + TypeScript + Vite** para consumir la API de TicketYa a través del
`api-gateway`.

La guía completa de ejecución del backend, datos de prueba, endpoints y colección de Postman está
en el [README raíz](../README.md).

## Flujos cubiertos

- Buscar eventos por artista, ciudad, tipo y rango de fechas.
- Ver el detalle del evento y del recinto.
- Elegir sector y asientos numerados (hasta 4 entradas por compra).
- Reservar asientos con clave de idempotencia y cuenta regresiva de 10 minutos.
- Confirmar el pago contra la pasarela mock.
- Ver las entradas emitidas con su código QR.
- Buscar entradas por ID de orden.
- Identidad de cliente simple (sin login), persistida en `localStorage`.

## Requisitos

- Node.js 20 o superior.
- Backend de TicketYa corriendo: MongoDB, PostgreSQL y los cinco servicios.
- `api-gateway` escuchando en `http://localhost:8080`.

Seguí el [inicio rápido del README raíz](../README.md#inicio-rápido) para levantar las bases y los
microservicios.

## Inicio rápido

```bash
cd frontend
npm install
npm run dev
```

Abrí `http://localhost:5173`.

No abras `index.html` directamente con el navegador ni con Live Server: la app usa módulos de Vite
y el proxy `/api`. Abrir el archivo por `file://` produce errores de CORS y recursos no encontrados.

## Configuración

Vite hace proxy de `/api` hacia `http://localhost:8080`, así que en desarrollo no hace falta
configurar CORS.

Para cambiar el destino:

```bash
cp .env.example .env.local
# editá VITE_GATEWAY_URL
```

Variables disponibles:

| Variable | Default | Descripción |
| --- | --- | --- |
| `VITE_GATEWAY_URL` | `http://localhost:8080` | Destino del proxy de Vite para `/api`. |
| `VITE_API_BASE_URL` | vacío | Si se define, el navegador llama directo a esa URL. Requiere CORS en el gateway. |
| `VITE_MAX_ENTRADAS` | `4` | Máximo de entradas por compra en la UI. Debe coincidir con `compras.limite-entradas-por-cliente`. |

Si corrés el frontend en WSL y el backend en Windows, revisá la sección
[WSL no llega al gateway de Windows](../README.md#wsl-no-llega-al-gateway-de-windows).

## Scripts

```bash
npm run dev      # servidor de desarrollo
npm run build    # type-check + build de producción
npm run preview  # preview del build
npm run lint     # oxlint
```

## Estructura

```text
src/
|-- api/          # Cliente HTTP del gateway
|-- components/   # Componentes reutilizables
|-- context/      # Identidad del cliente
|-- lib/          # Almacenamiento local y utilidades
|-- pages/        # Cartelera, detalle, checkout y mis entradas
|-- types.ts      # Tipos de la API
|-- main.tsx
`-- index.css
```

## Notas y limitaciones actuales

- **Sin autenticación**: el ID de cliente es la identidad. Se puede cambiar desde el botón
  “Cliente” del encabezado.
- **Sectores de capacidad general**: el backend de compras todavía solo reserva asientos
  numerados. La UI muestra estos sectores como informativos.
- **Pago simulado**: la pasarela mock no cobra dinero real.
- **Entradas por cliente**: se usa `GET /api/entradas/cliente/{clienteId}` en `entradas-service`.
  Si ese servicio no está recompilado, “Mis entradas” muestra las entradas guardadas localmente.
- El máximo de entradas por compra se puede ajustar con `VITE_MAX_ENTRADAS` y debe coincidir
  con `compras.limite-entradas-por-cliente`.
- La UI traduce los IDs de sector y lugar a nombres usando el catálogo de eventos y lugares. Si el
  backend de entradas guarda un ID como `sectorNombre`, la UI intenta mostrar el nombre legible
  cuando tiene datos del evento.
