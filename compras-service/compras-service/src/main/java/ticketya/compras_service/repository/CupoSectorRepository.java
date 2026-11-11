package ticketya.compras_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ticketya.compras_service.model.CupoSector;

import java.util.Optional;
import java.util.UUID;

public interface CupoSectorRepository extends JpaRepository<CupoSector, UUID> {

    Optional<CupoSector> findByEventoIdAndSectorId(String eventoId, String sectorId);

    @Modifying
    @Query("UPDATE CupoSector c SET c.cuposDisponibles = c.cuposDisponibles - :cantidad " +
            "WHERE c.eventoId = :eventoId AND c.sectorId = :sectorId AND c.cuposDisponibles >= :cantidad")
    int decrementarCupo(@Param("eventoId") String eventoId,
                        @Param("sectorId") String sectorId,
                        @Param("cantidad") int cantidad);

    @Modifying
    @Query("UPDATE CupoSector c SET c.cuposDisponibles = LEAST(c.capacidadTotal, c.cuposDisponibles + :cantidad) " +
            "WHERE c.eventoId = :eventoId AND c.sectorId = :sectorId")
    int incrementarCupo(@Param("eventoId") String eventoId,
                        @Param("sectorId") String sectorId,
                        @Param("cantidad") int cantidad);
}

