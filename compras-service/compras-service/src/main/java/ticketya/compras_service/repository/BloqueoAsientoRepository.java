package ticketya.compras_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ticketya.compras_service.model.BloqueoAsiento;
import ticketya.compras_service.model.EstadoBloqueo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BloqueoAsientoRepository extends JpaRepository<BloqueoAsiento, UUID> {

    List<BloqueoAsiento> findByOrdenId(UUID ordenId);

    List<BloqueoAsiento> findByEstadoAndExpiraEnBefore(EstadoBloqueo estado, LocalDateTime ahora);

    void deleteByOrdenId(UUID ordenId);

    void deleteByEventoIdAndSectorIdAndFilaAndAsiento(String eventoId, String sectorId, String fila, Integer asiento);
}