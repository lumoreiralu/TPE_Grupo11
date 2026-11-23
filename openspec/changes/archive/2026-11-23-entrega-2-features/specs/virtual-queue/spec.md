# Spec Delta

## Purpose

Ordena el acceso a la compra cuando un evento supera el umbral de demanda, asignando turnos correlativos y pases temporales que evitan saturar los servicios de reserva.

## ADDED Requirements

### Requirement: Asignación de turnos por alta demanda (R10.1)
While el volumen de peticiones concurrentes para un evento supere el umbral configurado, the system SHALL asignar a cada usuario un turno correlativo en la fila virtual.

#### Scenario: Umbral superado
- **WHEN** las peticiones concurrentes para un evento superan el umbral configurado
- **THEN** el sistema incorpora a cada usuario a la fila con un turno correlativo y le informa su posición

### Requirement: Pase temporal al llegar el turno (R10.2)
When llega el turno de un usuario en la fila, the system SHALL otorgarle un pase temporal de acceso para seleccionar entradas y avanzar a la compra.

#### Scenario: Turno alcanzado
- **WHEN** el turno de un usuario llega al frente de la fila
- **THEN** el sistema emite un pase temporal y habilita la selección de entradas durante su vigencia

### Requirement: Expiración del pase (R10.3)
If el usuario no inicia su compra dentro de la ventana de tiempo asignada a su turno, then the system SHALL invalidar su pase y llamar al siguiente usuario en espera.

#### Scenario: Pase vencido
- **WHEN** un usuario no inicia la compra antes de expirar la ventana de su turno
- **THEN** el sistema invalida el pase y habilita el turno del siguiente usuario de la fila
