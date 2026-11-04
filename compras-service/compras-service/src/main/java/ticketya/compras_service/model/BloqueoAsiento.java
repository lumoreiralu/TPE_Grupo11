package ticketya.compras_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bloqueos_asientos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bloqueo_clave_asiento",
                columnNames = "clave_asiento"))
@Getter
@Setter
@NoArgsConstructor
public class BloqueoAsiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String eventoId;

    @Column(nullable = false)
    private String sectorId;

    // Ej: "evento1|platea-a|A|3". UNICA: es lo que impide la doble venta
    @Column(name = "clave_asiento", nullable = false)
    private String claveAsiento;

    @Column(nullable = false)
    private UUID ordenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoBloqueo estado;

    @Column(nullable = false)
    private LocalDateTime expiraEn;
}