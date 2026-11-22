package ticketya.eventos_service.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ticketya.eventos_service.dto.PreferenciasRequest;
import ticketya.eventos_service.model.PreferenciasCliente;
import ticketya.eventos_service.model.TipoEvento;
import ticketya.eventos_service.repository.PreferenciasClienteRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PreferenciasServiceTest {

    @Test
    void guardaPreferenciasNormalizadas() {
        PreferenciasClienteRepository repository = mock(PreferenciasClienteRepository.class);
        when(repository.save(any(PreferenciasCliente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        PreferenciasService service = new PreferenciasService(repository);

        PreferenciasCliente resultado = service.guardar(" cliente1 ", new PreferenciasRequest(
                Set.of(TipoEvento.RECITAL, TipoEvento.TEATRO),
                Arrays.asList(" Banda B ", "banda b", " ", null, "Artista A"),
                " Rosario "));

        assertEquals("cliente1", resultado.getClienteId());
        assertEquals(Set.of(TipoEvento.RECITAL, TipoEvento.TEATRO), resultado.getTiposEvento());
        assertEquals(List.of("Banda B", "Artista A"), resultado.getArtistasFavoritos());
        assertEquals("Rosario", resultado.getCiudad());
        assertTrue(resultado.getFechaActualizacion() != null);
    }

    @Test
    void obtenerDevuelvePreferenciasVaciasCuandoNoExisten() {
        PreferenciasClienteRepository repository = mock(PreferenciasClienteRepository.class);
        when(repository.findById("cliente1")).thenReturn(Optional.empty());
        PreferenciasService service = new PreferenciasService(repository);

        PreferenciasCliente resultado = service.obtener("cliente1");

        assertEquals("cliente1", resultado.getClienteId());
        assertTrue(resultado.getTiposEvento().isEmpty());
        assertTrue(resultado.getArtistasFavoritos().isEmpty());
        assertNull(resultado.getCiudad());
    }

    @Test
    void rechazaIdentidadVacia() {
        PreferenciasClienteRepository repository = mock(PreferenciasClienteRepository.class);
        PreferenciasService service = new PreferenciasService(repository);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.obtener("  "));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }
}
