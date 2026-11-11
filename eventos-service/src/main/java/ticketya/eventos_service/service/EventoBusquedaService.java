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
import java.math.BigDecimal;
import java.util.Comparator;

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
                               LocalDate fechaHasta, String ordenarPor) {
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

        Query query = new Query(
        new Criteria().andOperator(filtros.toArray(new Criteria[0]))
);

List<Evento> eventos = mongoTemplate.find(query, Evento.class);

Comparator<Evento> porFecha = Comparator.comparing(
        Evento::getFechaHora,
        Comparator.nullsLast(Comparator.naturalOrder())
);

switch (ordenarPor == null ? "fecha" : ordenarPor.toLowerCase()) {

    case "precio_asc":
        eventos.sort(Comparator.comparing(
                this::obtenerPrecioMinimo,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));
        break;

    case "precio_desc":
        eventos.sort((a, b) -> {
            BigDecimal precioA = obtenerPrecioMinimo(a);
            BigDecimal precioB = obtenerPrecioMinimo(b);

            if (precioA == null) return precioB == null ? 0 : 1;
            if (precioB == null) return -1;

            return precioB.compareTo(precioA);
        });
        break;

    case "relevancia":
        eventos.sort(
                Comparator.comparingInt(
                        (Evento e) -> calcularRelevancia(e, artista)
                ).reversed().thenComparing(porFecha)
        );
        break;

    default:
        eventos.sort(porFecha);
        break;
}

return eventos;
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

private BigDecimal obtenerPrecioMinimo(Evento evento) {
    if (evento.getPreciosSectores() == null ||
            evento.getPreciosSectores().isEmpty()) {
        return null;
    }

    return evento.getPreciosSectores().stream()
            .map(Evento.PrecioSector::getPrecio)
            .filter(p -> p != null)
            .min(BigDecimal::compareTo)
            .orElse(null);
}

private int calcularRelevancia(Evento evento, String artista) {
    if (!tieneTexto(artista) || evento.getParticipantes() == null) {
        return 0;
    }

    String busqueda = artista.toLowerCase().trim();
    int puntuacion = 0;

    for (String participante : evento.getParticipantes()) {
        if (participante == null) continue;

        String nombre = participante.toLowerCase().trim();

        if (nombre.equals(busqueda)) {
            puntuacion = Math.max(puntuacion, 3);
        } else if (nombre.startsWith(busqueda)) {
            puntuacion = Math.max(puntuacion, 2);
        } else if (nombre.contains(busqueda)) {
            puntuacion = Math.max(puntuacion, 1);
        }
    }

    return puntuacion;
}
}