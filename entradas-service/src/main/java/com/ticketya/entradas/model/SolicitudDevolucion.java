package com.ticketya.entradas.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "solicitudes_devolucion")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudDevolucion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID entradaId;

    @Column(nullable = false)
    private String clienteId;

    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudDevolucion estado;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;
    private String respuestaAdmin;

    @PrePersist
    protected void onCreate() {
        this.fechaSolicitud = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoSolicitudDevolucion.PENDIENTE;
        }
    }
}
