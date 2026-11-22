package ticketya.eventos_service.dto;

import ticketya.eventos_service.model.TipoEvento;

import java.util.List;
import java.util.Set;

public record PreferenciasRequest(
        Set<TipoEvento> tiposEvento,
        List<String> artistasFavoritos,
        String ciudad
) {}
