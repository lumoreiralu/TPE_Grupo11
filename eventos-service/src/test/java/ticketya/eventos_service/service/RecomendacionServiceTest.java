package ticketya.eventos_service.service;

import org.junit.jupiter.api.Test;
import ticketya.eventos_service.client.EntradaHistorialDTO;
import ticketya.eventos_service.client.EntradasClient;
import ticketya.eventos_service.dto.RecomendacionResponse;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.PreferenciasCliente;
import ticketya.eventos_service.model.TipoEvento;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecomendacionServiceTest {

    @Test
    void recomiendaSegunPreferenciasHistorialYExcluyeEventosYaComprados() {
        CatalogoCacheService catalogo = mock(CatalogoCacheService.class);
        PreferenciasService preferenciasService = mock(PreferenciasService.class);
        EntradasClient entradasClient = mock(EntradasClient.class);
        RecomendacionService service =
                new RecomendacionService(catalogo, preferenciasService, entradasClient);

        PreferenciasCliente preferencias = new PreferenciasCliente();
        preferencias.setClienteId("cliente1");
        preferencias.setTiposEvento(new LinkedHashSet<>(List.of(TipoEvento.RECITAL)));
        preferencias.setArtistasFavoritos(List.of("Banda B"));
        preferencias.setCiudad("Rosario");
        when(preferenciasService.obtener("cliente1")).thenReturn(preferencias);

        Evento comprado = evento("evt-comprado", TipoEvento.TEATRO,
                List.of("Artista Historico"), "Cordoba", LocalDateTime.now().minusDays(10));
        when(catalogo.obtenerPorId("evt-comprado")).thenReturn(Optional.of(comprado));
        when(entradasClient.obtenerEntradasPorCliente("cliente1")).thenReturn(
                CompletableFuture.completedFuture(List.of(new EntradaHistorialDTO(
                        "evt-comprado", "cliente1", "EMITIDA",
                        LocalDateTime.now().minusDays(10)))));

        Evento favorito = evento("evt-favorito", TipoEvento.RECITAL,
                List.of("Banda B"), "Rosario", LocalDateTime.now().plusDays(20));
        Evento porHistorial = evento("evt-historico", TipoEvento.TEATRO,
                List.of("Artista Historico"), "Cordoba", LocalDateTime.now().plusDays(25));
        Evento sinRelacion = evento("evt-otro", TipoEvento.DEPORTE,
                List.of("Otro Artista"), "Montevideo", LocalDateTime.now().plusDays(30));
        when(catalogo.obtenerPublicados()).thenReturn(
                List.of(comprado, favorito, porHistorial, sinRelacion));

        List<RecomendacionResponse> resultado = service.recomendar("cliente1", 6);

        assertEquals(List.of("evt-favorito", "evt-historico"),
                resultado.stream().map(rec -> rec.evento().getId()).toList());
        assertTrue(resultado.get(0).puntaje() > resultado.get(1).puntaje());
        assertTrue(resultado.get(0).motivo().contains("Artista favorito: Banda B"));
        assertTrue(resultado.get(1).motivo().contains("Según tu historial"));
        assertFalse(resultado.stream().anyMatch(rec -> "evt-comprado".equals(rec.evento().getId())));
    }

    @Test
    void sinPreferenciasNiHistorialNoGeneraRecomendaciones() {
        CatalogoCacheService catalogo = mock(CatalogoCacheService.class);
        PreferenciasService preferenciasService = mock(PreferenciasService.class);
        EntradasClient entradasClient = mock(EntradasClient.class);
        RecomendacionService service =
                new RecomendacionService(catalogo, preferenciasService, entradasClient);

        PreferenciasCliente preferencias = new PreferenciasCliente();
        preferencias.setClienteId("cliente1");
        when(preferenciasService.obtener("cliente1")).thenReturn(preferencias);
        when(entradasClient.obtenerEntradasPorCliente("cliente1")).thenReturn(
                CompletableFuture.completedFuture(List.of()));
        when(catalogo.obtenerPublicados()).thenReturn(List.of(
                evento("evt-1", TipoEvento.RECITAL, List.of("Artista"), "Rosario",
                        LocalDateTime.now().plusDays(10))));

        assertTrue(service.recomendar("cliente1", 6).isEmpty());
    }

    private Evento evento(String id, TipoEvento tipo, List<String> participantes,
                          String ciudad, LocalDateTime fechaHora) {
        Evento evento = new Evento();
        evento.setId(id);
        evento.setNombre(id);
        evento.setTipoEvento(tipo);
        evento.setParticipantes(participantes);
        evento.setCiudad(ciudad);
        evento.setFechaHora(fechaHora);
        return evento;
    }
}
