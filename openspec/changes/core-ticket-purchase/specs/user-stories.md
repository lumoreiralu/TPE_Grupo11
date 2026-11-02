# Especificaciones del Sistema (Historias de Usuario en EARS)

## Feature 1: Gestión de Lugares y Eventos 
### US-01: Definición de Lugares y Sectores 
**Como** Organizador / Administrador, 
**Quiero** registrar recintos con sus sectores numerados o de capacidad general,
**Para** configurar la distribución de localidades. 

- **R1.1:** **When** un organizador crea un lugar, **the system shall** registrar su ID, nombre, dirección y coordenadas geográficas. 
- **R1.2:** **While** se define un sector numerado, **the system shall** exigir la especificación de filas y asientos. 
- **R1.3:** **While** se define un sector no numerado, **the system shall** exigir una capacidad máxima permitida. 

- ### US-02: Creación y Publicación de Eventos 
**Como** Organizador,
**Quiero** crear eventos en borrador, asignar precios por sector y publicarlos, 
**Para** poner las entradas a la venta. 
- **R2.1:** **When** un organizador crea un evento, **the system shall** iniciarlo en estado `BORRADOR`. 
- **R2.2:** **When** el organizador asigna precios a los sectores y confirma, **the system shall** cambiar el estado a `PUBLICADO`. 
- **R2.3:** **If** un evento está en estado `BORRADOR`, **then the system shall** impedir la selección de entradas por los clientes. 

## Feature 2: Búsqueda de Eventos 
### US-03: Búsqueda Multi-criterio 
**Como** Cliente,
**Quiero** buscar y filtrar eventos por categoría, lugar y fechas, 
**Para** encontrar espectáculos de mi interés. 
- **R3.1:** **When** un cliente consulta el catálogo, **the system shall** permitir filtros combinados por tipo de evento, organizador, artista, ciudad y rango de fechas. 


## Feature 3: Reserva, Compra y Prevención de Doble Venta 
### US-04: Reserva Temporal de Entradas 
**Como** Cliente, 
**Quiero** seleccionar lugares y mantenerlos reservados por 10 minutos, 
**Para** ingresar mi medio de pago sin perder la selección. 
- **R4.1:** **When** un cliente selecciona asientos o lugares disponibles, **the system shall** generar una reserva temporal por 10 minutos y bloquear la selección a otros usuarios. 
- **R4.2:** **If** transcurren 10 minutos sin confirmación de pago, **then the system shall** liberar automáticamente los lugares.

### US-05: Procesamiento de Pago y Emisión de QR 
**Como** Cliente,
**Quiero** pagar mi reserva y recibir la entrada con un código QR,
**Para** asegurar mi acceso al evento. 
- **R5.1 (Concurrencia):** **When** múltiples clientes intentan reservar el mismo asiento en paralelo, **the system shall** garantizar la asignación a una única solicitud mediante control de concurrencia e idempotencia. 
- **R5.2 (Pago Exitoso):** **When** la pasarela confirma el cobro dentro del tiempo de reserva, **the system shall** emitir la entrada con un código QR único. 
- **R5.3 (Pago Rechazado):** **If** la pasarela rechaza la transacción, **then the system shall** liberar inmediatamente los lugares reservados.