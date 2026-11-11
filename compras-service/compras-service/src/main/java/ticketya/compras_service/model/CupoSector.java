package ticketya.compras_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "cupos_sectores",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cupo_evento_sector",
                columnNames = {"evento_id", "sector_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CupoSector {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evento_id", nullable = false)
    private String eventoId;

    @Column(name = "sector_id", nullable = false)
    private String sectorId;

    @Column(name = "capacidad_total", nullable = false)
    private Integer capacidadTotal;

    @Column(name = "cupos_disponibles", nullable = false)
    private Integer cuposDisponibles;

    public CupoSector(String eventoId, String sectorId, Integer capacidadTotal, Integer cuposDisponibles) {
        this.eventoId = eventoId;
        this.sectorId = sectorId;
        this.capacidadTotal = capacidadTotal;
        this.cuposDisponibles = cuposDisponibles;
    }
}

