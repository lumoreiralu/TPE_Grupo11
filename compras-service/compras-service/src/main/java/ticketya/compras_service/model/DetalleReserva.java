package ticketya.compras_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "detalles_reserva")
@Getter
@Setter
@NoArgsConstructor
public class DetalleReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String sectorId;

    // Nulos si el sector es de capacidad general
    private String fila;
    private Integer asiento;

    @Column(nullable = false)
    private BigDecimal precioUnitario;
}