package ticketya.eventos_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ticketya.eventos_service.model.Lugar;
@Repository
public interface LugarRepository extends MongoRepository<Lugar, String> {
}
