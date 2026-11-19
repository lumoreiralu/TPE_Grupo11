# Spec Delta

## Purpose

Mantiene la consistencia entre servicios mediante una saga orquestada con compensaciones explícitas y hace que las dependencias remotas fallidas no dejen operaciones inconclusas ni局部mente confirmadas.

## ADDED Requirements

### Requirement: Compensación ante fallo de emisión (RES-1)
When la emisión de entradas falla después de confirmado el pago, the system SHALL compensar la operación solicitando el reintegro, liberando los asientos y dejando la orden en estado consistente.

#### Scenario: Emisión fallida
- **WHEN** `entradas-service` rechaza la emisión de una orden ya pagada
- **THEN** la saga solicita el reembolso a la pasarela, libera los bloqueos y marca la orden como compensada

### Requirement: Recuperación de operaciones pendientes (RES-2)
If una orden permanece sin resolución en estado `PROCESANDO_PAGO`, then the system SHALL consultar la pasarela y converger a un estado final aplicando la compensación correspondiente.

#### Scenario: Orden pendiente tras caída
- **WHEN** una tarea periódica encuentra una orden en `PROCESANDO_PAGO` más allá del tiempo esperado
- **THEN** el sistema consulta el estado real del pago y confirma o compensa la orden según corresponda

### Requirement: Fallback ante dependencia no disponible (RES-3)
When una dependencia remota falla o excede el tiempo de espera, the system SHALL reintentar con espera progresiva, abrir un cortocircuito temporal y ejecutar una acción alternativa que preserve la consistencia.

#### Scenario: Pasarela sin respuesta
- **WHEN** la pasarela no responde dentro del tiempo configurado durante varias llamadas
- **THEN** el sistema deja de invocarla temporalmente, responde con un error controlado y mantiene la reserva o inicia la compensación

### Requirement: Idempotencia de reintentos (RES-4)
When se repite una operación con la misma clave de idempotencia, the system SHALL devolver el resultado original sin duplicar reservas, cargos, entradas ni reembolsos.

#### Scenario: Reintento de confirmación de pago
- **WHEN** llega dos veces la misma confirmación de pago con idéntica clave de idempotencia
- **THEN** el sistema procesa una sola vez y devuelve el mismo resultado en ambas respuestas
