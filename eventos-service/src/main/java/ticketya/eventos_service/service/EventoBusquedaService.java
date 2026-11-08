package ticketya.eventos_service.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.TipoEvento;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class EventoBusquedaService {

    private final MongoTemplate mongoTemplate;

    public EventoBusquedaService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<Evento> buscar(TipoEvento tipo,
                               String organizadorId,
                               String artista,
                               String ciudad,
                               LocalDate fechaDesde,
                               LocalDate fechaHasta) {
        List<Criteria> filtros = new ArrayList<>();
        filtros.add(Criteria.where("estado").is(EstadoEvento.PUBLICADO));

        if (tipo != null) {
            filtros.add(Criteria.where("tipoEvento").is(tipo));
        }
        if (tieneTexto(organizadorId)) {
            filtros.add(Criteria.where("organizadorId").is(organizadorId));
        }
        if (tieneTexto(artista)) {
            filtros.add(Criteria.where("participantes").regex(Pattern.quote(artista), "i"));
        }
        if (tieneTexto(ciudad)) {
            filtros.add(Criteria.where("ciudad").regex("^" + Pattern.quote(ciudad) + "$", "i"));
        }
        if (fechaDesde != null || fechaHasta != null) {
            Criteria fecha = Criteria.where("fechaHora");
            if (fechaDesde != null) {
                fecha.gte(fechaDesde.atStartOfDay());
            }
            if (fechaHasta != null) {
                fecha.lte(fechaHasta.atTime(LocalTime.MAX));
            }
            filtros.add(fecha);
        }

        Query query = new Query(new Criteria().andOperator(filtros.toArray(new Criteria[0])))
                .with(Sort.by("fechaHora").ascending());

        return mongoTemplate.find(query, Evento.class);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
