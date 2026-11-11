package ticketya.eventos_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;

import java.util.List;

@Repository
public interface EventoRepository extends MongoRepository<Evento, String> {
    List<Evento> findByEstado(EstadoEvento estado);
}
