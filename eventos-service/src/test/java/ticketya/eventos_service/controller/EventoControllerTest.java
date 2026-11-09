package ticketya.eventos_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.repository.EventoRepository;
import ticketya.eventos_service.repository.LugarRepository;
import ticketya.eventos_service.service.EventoBusquedaService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
    private LugarRepository lugarRepository;

    @MockitoBean
    private EventoBusquedaService busquedaService;

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
    }
}

