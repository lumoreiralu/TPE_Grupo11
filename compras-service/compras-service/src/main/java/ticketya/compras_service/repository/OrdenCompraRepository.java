package ticketya.compras_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.model.OrdenCompra;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, UUID> {

    Optional<OrdenCompra> findByIdempotencyKey(String idempotencyKey);

    List<OrdenCompra> findByEstadoAndFechaExpiracionReservaBefore(EstadoOrden estado, LocalDateTime ahora);

    @Query("select count(d) from OrdenCompra o join o.detalles d " +
            "where o.clienteId = :clienteId and o.eventoId = :eventoId and o.estado in :estados")
    long contarEntradasDelCliente(@Param("clienteId") String clienteId,
                                  @Param("eventoId") String eventoId,
                                  @Param("estados") List<EstadoOrden> estados);
}