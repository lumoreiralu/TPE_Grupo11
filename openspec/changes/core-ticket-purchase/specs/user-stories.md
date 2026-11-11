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

## Feature 4: Devoluciones y Cancelaciones (RF-07) 
### US-06: Solicitud de Devolución por el Cliente 
**Como** Cliente,
**Quiero** solicitar la devolución de una entrada comprada con al menos 72 horas de anticipación, 
**Para** recuperar mi dinero si no puedo asistir al evento. 
- **R6.1:** 
- **When** un cliente solicita la devolución de una entrada con más de 72 horas de anticipación al inicio del evento, **the system shall** registrar la solicitud en estado `PENDIENTE`. 
- **R6.2:** **If** un cliente solicita la devolución con menos de 72 horas de anticipación al inicio del evento, **then the system shall** rechazar la solicitud informando que el plazo expiró. 
- **R6.3:** **If** la entrada no se encuentra en estado `EMITIDA` (ya fue usada o devuelta), **then the system shall** impedir la creación de la solicitud.

### US-07: Aprobación Administrativa y Reembolso 
**Como** Administrador, 
**Quiero** revisar, aprobar o rechazar las solicitudes de devolución pendientes,
**Para** autorizar el reintegro de fondos y poner las localidades nuevamente a la venta. 
- **R7.1:** **When** un administrador aprueba una solicitud de devolución, **the system shall** cambiar la entrada a estado `CANCELADA`, liberar inmediatamente el asiento o cupo correspondiente y ordenar el reintegro a la pasarela de pagos. 
- **R7.2:** **When** un administrador rechaza una solicitud de devolución, **the system shall** registrar el motivo de rechazo y mantener la entrada en estado `EMITIDA`. 

### US-08: Cancelación Masiva de Evento 
**Como** Administrador / Organizador,
**Quiero** cancelar un evento publicado o en venta,
**Para** suspender la comercialización y reembolsar automáticamente a los compradores. 
- **R8.1:** **When** se confirma la cancelación de un evento, **the system shall** cambiar su estado a `CANCELADO` e impedir cualquier nueva reserva.
- **R8.2:** **When** un evento pasa a estado `CANCELADO`, **the system shall** transicionar automáticamente todas sus entradas vendidas a `CANCELADA`, liberar los asientos/cupos y disparar la solicitud de reembolso a la pasarela de pagos. 

## Feature 5: Control de Acceso (RF-08)
### US-09: Validación de Entrada en Puerta 
**Como** Personal de Control de Acceso,
**Quiero** escanear el código QR de una entrada en el ingreso al recinto,
**Para** verificar su autenticidad y habilitar el paso únicamente a portadores de tickets válidos.
- **R9.1:** **When** se escanea un código QR en estado `EMITIDA` que corresponde al evento del día, **the system shall** cambiar su estado a `USADA` y autorizar el acceso. 
- **R9.2:** **If** se escanea un código QR cuya entrada ya figura en estado `USADA`, **then the system shall** rechazar el acceso alertando sobre intento de reingreso fraudulento.
- **R9.3:** **If** el código QR no existe, pertenece a otro evento o está en estado `CANCELADA`, **then the system shall** denegar el acceso.

## Feature 6: Gestión de Demanda Extrema (RF-06) 
### US-10: Fila Virtual para Eventos de Alta Concurrencia 
**Como** Cliente, 
**Quiero** ingresar a una sala de espera / fila virtual al momento de apertura de ventas, 
**Para** comprar de manera ordenada según mi orden de llegada sin saturar la plataforma.
- **R10.1:** **While** el volumen de peticiones concurrentes para un evento supere el umbral configurado, **the system shall** asignar a cada usuario un turno correlativo en la fila virtual. 
- **R10.2:** **When** llega el turno de un usuario en la fila, **the system shall** otorgarle un pase temporal de acceso para seleccionar entradas y avanzar a la compra. 
- **R10.3:** **If** el usuario no inicia su compra dentro de la ventana de tiempo asignada a su turno, **then the system shall** invalidar su pase y llamar al siguiente usuario en espera.

## Feature 7: Notificaciones y Comunicaciones (RF-11) 
### US-11: Notificaciones Transaccionales y de Evento 
**Como** Cliente,
**Quiero** recibir notificaciones por correo electrónico ante eventos relevantes sobre mis compras, 
**Para** tener un comprobante y mantenerme al tanto de cualquier cambio de programación. 
- **R11.1:** **When** se completa exitosamente el pago de una orden, **the system shall** enviar un correo al cliente con la confirmación y el detalle de las entradas emitidas. 
- **R11.2:** **When** un evento cambia de horario, lugar o es cancelado, **the system shall** enviar una notificación a todos los clientes con entradas adquiridas para ese evento. 

## Feature 8: Reportes y Métricas de Ocupación (RF-12) 
### US-12: Consulta de Métricas de Venta y Recaudación 
**Como** Organizador, 
**Quiero** visualizar estadísticas de recaudación y ocupación en tiempo real para mis eventos, 
**Para** evaluar el rendimiento comercial y la disponibilidad por sector. 
- **R12.1:** **When** un organizador consulta el panel de métricas de su evento, **the system shall** calcular y mostrar la recaudación total acumulada. 
- **R12.2:** **When** se solicitan los reportes de ocupación, **the system shall** desglosar el porcentaje y cantidad de entradas vendidas, disponibles y devueltas por cada sector.