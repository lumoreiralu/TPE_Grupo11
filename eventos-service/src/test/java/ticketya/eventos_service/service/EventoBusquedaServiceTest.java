package ticketya.eventos_service.service;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.TipoEvento;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventoBusquedaServiceTest {

    @Test
    void siempreFiltraEventosPublicadosYOrdenaPorFecha() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        EventoBusquedaService service = new EventoBusquedaService(mongoTemplate);
        Evento evento = new Evento();
        when(mongoTemplate.find(any(Query.class), eq(Evento.class))).thenReturn(List.of(evento));

        List<Evento> resultado = service.buscar(null, null, null, null, null, null, "fecha");
        assertEquals(List.of(evento), resultado);
        Query query = capturarQuery(mongoTemplate);
        List<Document> condiciones = condiciones(query);
        assertEquals(1, condiciones.size());
        assertEquals("estado", condiciones.get(0).keySet().iterator().next());
        assertEquals(EstadoEvento.PUBLICADO, condiciones.get(0).get("estado"));
        assertNotNull(query.getSortObject());
        assertEquals("fechaHora", query.getSortObject().keySet().iterator().next());
    }

    @Test
    void combinaLosFiltrosOpcionales() {
        MongoTemplate mongoTemplate = mock(MongoTemplate.class);
        EventoBusquedaService service = new EventoBusquedaService(mongoTemplate);
        when(mongoTemplate.find(any(Query.class), eq(Evento.class))).thenReturn(List.of());
        service.buscar(TipoEvento.RECITAL, "org-1", "Artista", "Rosario",
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31), "fecha");

        Query query = capturarQuery(mongoTemplate);
        assertEquals(TipoEvento.RECITAL, valorDe(query, "tipoEvento"));
        assertEquals("org-1", valorDe(query, "organizadorId"));
        assertTrue(contieneCampo(query, "participantes"));
        assertTrue(contieneCampo(query, "ciudad"));
        assertTrue(contieneCampo(query, "fechaHora"));
    }

    private Query capturarQuery(MongoTemplate mongoTemplate) {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Evento.class));
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<Document> condiciones(Query query) {
        return (List<Document>) query.getQueryObject().get("$and");
    }

    private boolean contieneCampo(Query query, String campo) {
        return condiciones(query).stream().anyMatch(condicion -> condicion.containsKey(campo));
    }

    private Object valorDe(Query query, String campo) {
        return condiciones(query).stream()
                .filter(condicion -> condicion.containsKey(campo))
                .map(condicion -> condicion.get(campo))
                .findFirst()
                .orElse(null);
    }
}
