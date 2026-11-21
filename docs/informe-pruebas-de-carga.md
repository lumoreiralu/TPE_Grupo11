# Informe de Pruebas de Carga y Rendimiento (JMeter) — TicketYa (Entrega 2)

## 1. Objetivo y Escenario de Prueba
El objetivo de esta prueba de carga es evaluar el comportamiento del backend de TicketYa ante un escenario de alta concurrencia simulando la **apertura de venta de un evento masivo**, conforme a los requisitos no funcionales de la **Entrega 2 (TUDAI)**.

La prueba fue ejecutada utilizando **Apache JMeter 5.5** empaquetado en contenedor Docker e interconectado directamente a la red de microservicios (`tpe_grupo11_default`), consumiendo los endpoints exclusivamente a través del **API Gateway** (`http://api-gateway:8080`).

---

## 2. Parámetros de la Prueba
* **Herramienta:** Apache JMeter 5.5 (Headless CLI).
* **Punto de entrada:** API Gateway (`http://api-gateway:8080`).
* **Usuarios Concurrentes (Hilos):** 50 usuarios en paralelo.
* **Ramp-up:** 3 segundos.
* **Muestras totales procesadas:** 1.040 transacciones HTTP.
* **Duración total:** ~4 segundos.
* **Throughput global:** **341 transacciones/segundo**.

---

## 3. Resultados Cuantitativos

| Transacción / Endpoint | Muestras | Tiempo Promedio (ms) | Mediana (ms) | Min (ms) | Max (ms) | Perc. 95 (p95) | Throughput (req/s) | % Error |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `GET /api/eventos/publicados` (Catálogo Caché) | 500 | 89.6 ms | 68.0 ms | 13.0 ms | 560.0 ms | **242.0 ms** | **164.25 req/s** | **0.00%** |
| `POST /api/auth/login` (Autenticación JWT) | 20 | 322.6 ms | 298.0 ms | 162.0 ms | 498.0 ms | **497.1 ms** | **8.12 req/s** | **0.00%** |
| `POST /api/compras/reservar` (Reserva Concurrente) | 20 | 268.7 ms | 234.0 ms | 129.0 ms | 630.0 ms | **621.8 ms** | **9.74 req/s** | *95.00% (Esperado)* |
| **TOTAL CONSOLIDADO** | **1.040** | **67.3 ms** | **42.0 ms** | **4.0 ms** | **630.0 ms** | **242.0 ms** | **341.10 req/s** | — |

---

## 4. Análisis de Rendimiento y Hallazgos para la Defensa Oral

### A. Impacto de la Caché Distribuida (Redis) en el Catálogo
* El endpoint de consulta de eventos publicados (`/api/eventos/publicados`) atendió **500 solicitudes con 0% de errores**, alcanzando una tasa de procesamiento de más de **164 peticiones por segundo**.
* Gracias a la caché Redis con TTL y deshidratación en memoria, las latencias se mantuvieron por debajo de los **68 ms en mediana** (y mínimos de 13 ms), evitando saturar el clúster de MongoDB (`db_eventos`).
* El percentil 95 (p95) se ubicó en **242 ms**, demostrando una experiencia de usuario sumamente fluida incluso bajo ráfagas intensas de tráfico.

### B. Comportamiento del Mecanismo Anti-Doble Venta
* Se sometió al sistema a una ráfaga de 20 peticiones simultáneas intentando reservar **exactamente el mismo asiento** en el mismo milisegundo.
* **Resultado:** 1 solicitud resultó exitosa (`HTTP 201 Created`) y las 19 restantes fueron rechazadas inmediatamente por el motor relacional con código de conflicto (`HTTP 409 Conflict`).
* En JMeter, los códigos HTTP 4xx se computan contablemente como respuestas de error (95%), pero a nivel de arquitectura y lógica de negocio representa el **comportamiento esperado y correcto**: se garantizó de forma estricta que **ninguna entrada se venda dos veces**.

### C. Sobrecarga Criptográfica de Autenticación
* El endpoint `/api/auth/login` presentó tiempos de respuesta en torno a los 300 ms debido al cómputo de hashing con **BCrypt** (costo computacional intencional para prevenir ataques de fuerza bruta) y la firma digital HMAC-SHA256 del token JWT.

---

## 5. Ubicación de los Artefactos Generados
* **Plan de pruebas JMeter (parametrizable):** `tests/load/jmeter/entrega2.jmx`
* **Log crudo de resultados:** `tests/load/jmeter/results.jtl`
* **Reporte HTML interactivo generado:** `tests/load/jmeter/report/index.html` (abrir en el navegador para visualizar gráficos de throughput, tiempos de respuesta en el tiempo y percentiles).
