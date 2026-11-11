# Proposal: Core de Venta y Reserva de Entradas (Entrega 1)
## 1. Alcance y Contexto
Implementar la funcionalidad central (*Core*) del sistema **TicketYa** para garantizar el flujo completo de venta de entradas sin doble reserva/venta. 
El sistema debe gestionar el catálogo de eventos/lugares, la reserva temporal de entradas (10 minutos), la simulación del cobro con pasarela de pagos y la emisión del ticket digital con código QR único.

## 2. Objetivos Principales 
- Garantizar la prevención de doble reserva/venta de asientos numerados y cupos generales mediante mecanismos de concurrencia e idempotencia. 
- Manejar el ciclo de vida de la reserva temporal (10 minutos con liberación automática por timeout). 
- Permitir la búsqueda y publicación de eventos con tarifas por sector. 
- Generar y emitir el código QR del ticket una vez confirmado el pago.

## 3. Fuera de Alcance (Entrega 2) 
- Seguridad avanzada con JWT/OAuth2 y Roles granulares en API Gateway. 
- Caché distribuida con Redis para lectura masiva de catálogo. 
- Pruebas de resistencia/carga masiva con JMeter/Gatling y resiliencia (Circuit Breaker).