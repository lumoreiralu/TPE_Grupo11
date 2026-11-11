package com.ticketya.entradas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "entradas")
@Getter
@Setter
@NoArgsConstructor
public class Entrada {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String ordenCompraId;

    @Column(nullable = false)
    private String clienteId;

    @Column(nullable = false)
    private String eventoId;

    @Column(nullable = false)
    private String sectorNombre;

    private String fila;

    private Integer asiento;

    @Column(nullable = false, unique = true)
    private String codigoQrHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoEntrada estado;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    @PrePersist
    protected void onCreate() {
        this.fechaEmision = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoEntrada.EMITIDA;
        }
    }
}
