package ticketya.eventos_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ticketya.eventos_service.client.EntradasClient;
import ticketya.eventos_service.dto.PreferenciasRequest;
import ticketya.eventos_service.dto.RecomendacionResponse;
import ticketya.eventos_service.messaging.DomainEvent;
import ticketya.eventos_service.messaging.DomainEventPublisher;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.PreferenciasCliente;
import ticketya.eventos_service.model.TipoEvento;
import ticketya.eventos_service.repository.EventoRepository;
import ticketya.eventos_service.service.CatalogoCacheService;
import ticketya.eventos_service.service.PreferenciasService;
import ticketya.eventos_service.service.RecomendacionService;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventoController.class)
class EventoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventoRepository eventoRepository;

    @MockitoBean
    private CatalogoCacheService catalogoCacheService;

    @MockitoBean
    private EntradasClient entradasClient;

    @MockitoBean
    private DomainEventPublisher eventPublisher;

    @MockitoBean
    private PreferenciasService preferenciasService;

    @MockitoBean
    private RecomendacionService recomendacionService;

    @Test
    void rechazaPublicarEventoSinPreciosPorSector() throws Exception {
        Evento eventoSinPrecios = new Evento();
        eventoSinPrecios.setId("evt-1");
        eventoSinPrecios.setNombre("Recital de Rock");
        eventoSinPrecios.setEstado(EstadoEvento.BORRADOR);

        when(eventoRepository.findById("evt-1")).thenReturn(Optional.of(eventoSinPrecios));

        mockMvc.perform(put("/api/eventos/evt-1/publicar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value("No se puede publicar el evento: debe tener precios definidos por sector"));
    }

    @Test
    void cancelarEventoInvalidaElCatalogo() throws Exception {
        Evento evento = new Evento();
        evento.setId("evt-3");
        evento.setEstado(EstadoEvento.PUBLICADO);
        when(eventoRepository.findById("evt-3")).thenReturn(Optional.of(evento));
        when(entradasClient.cancelarEntradasPorEvento("evt-3"))
                .thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/api/eventos/evt-3/cancelar"))
                .andExpect(status().isOk());

        verify(catalogoCacheService).invalidarCatalogo();
        verify(eventPublisher).publish(any(DomainEvent.class));
    }

    @Test
    void publicaEventoConPreciosValidos() throws Exception {
        Evento eventoConPrecios = new Evento();
        eventoConPrecios.setId("evt-2");
        eventoConPrecios.setNombre("Teatro Clásico");
        eventoConPrecios.setEstado(EstadoEvento.BORRADOR);
        eventoConPrecios.setPreciosSectores(List.of(
                new Evento.PrecioSector("sector-1", new BigDecimal("5000.00"))
        ));

        when(eventoRepository.findById("evt-2")).thenReturn(Optional.of(eventoConPrecios));
        when(eventoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/api/eventos/evt-2/publicar")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PUBLICADO"));

        verify(catalogoCacheService).invalidarCatalogo();
    }

    @Test
    void guardaPreferenciasUsandoLaIdentidadDelHeader() throws Exception {
        PreferenciasCliente guardadas = new PreferenciasCliente();
        guardadas.setClienteId("cliente1");
        guardadas.setTiposEvento(new LinkedHashSet<>(Set.of(TipoEvento.RECITAL)));
        guardadas.setArtistasFavoritos(List.of("Banda B"));
        guardadas.setCiudad("Rosario");
        when(preferenciasService.guardar(eq("cliente1"), any(PreferenciasRequest.class)))
                .thenReturn(guardadas);

        mockMvc.perform(put("/api/eventos/preferencias")
                        .header("X-User-Id", "cliente1")
                        .param("clienteId", "otro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tiposEvento": ["RECITAL"],
                                  "artistasFavoritos": ["Banda B"],
                                  "ciudad": "Rosario"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value("cliente1"))
                .andExpect(jsonPath("$.ciudad").value("Rosario"));

        verify(preferenciasService).guardar(eq("cliente1"), any(PreferenciasRequest.class));
    }

    @Test
    void devuelveRecomendacionesDelClienteAutenticado() throws Exception {
        Evento recomendado = new Evento();
        recomendado.setId("evt-1");
        recomendado.setNombre("Festival Rock");
        when(recomendacionService.recomendar("cliente1", 2)).thenReturn(List.of(
                new RecomendacionResponse(recomendado, "Artista favorito: Banda B", 14)));

        mockMvc.perform(get("/api/eventos/recomendados")
                        .header("X-User-Id", "cliente1")
                        .param("clienteId", "otro")
                        .param("limite", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].evento.id").value("evt-1"))
                .andExpect(jsonPath("$[0].motivo").value("Artista favorito: Banda B"))
                .andExpect(jsonPath("$[0].puntaje").value(14));

        verify(recomendacionService).recomendar("cliente1", 2);
    }
}

