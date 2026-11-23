# ADR 0012: Justificación del uso de MongoDB (NoSQL) para el Catálogo 

- **Estado:** Aceptado 
- **Fecha:** 2026-11-22 
- **Microservicio:** `eventos-service` 

## Contexto 
Se requiere persistencia políglota para el catálogo de eventos, separando las necesidades de lectura de alta frecuencia de las transacciones ACID de compra. 

## Decisión 
Utilizar MongoDB (`db_eventos`) como base de datos orientada a documentos. 

## Justificación 
- **Esquema flexible (Schema-less): 
** Permite almacenar lugares y eventos con sectores y precios embebidos en JSON/BSON sin realizar JOINs complejos . 
- **Alto rendimiento de lectura:
** Optimiza las búsquedas multicriterio de eventos públicos.