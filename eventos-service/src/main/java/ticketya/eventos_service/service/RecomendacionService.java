package ticketya.eventos_service.service;

import org.springframework.stereotype.Service;
import ticketya.eventos_service.client.EntradaHistorialDTO;
import ticketya.eventos_service.client.EntradasClient;
import ticketya.eventos_service.dto.RecomendacionResponse;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.PreferenciasCliente;
import ticketya.eventos_service.model.TipoEvento;
import ticketya.eventos_service.util.Futures;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecomendacionService {

    private static final int MAX_RECOMENDACIONES = 20;

    private final CatalogoCacheService catalogoCacheService;
    private final PreferenciasService preferenciasService;
    private final EntradasClient entradasClient;

    public RecomendacionService(
            CatalogoCacheService catalogoCacheService,
            PreferenciasService preferenciasService,
            EntradasClient entradasClient) {
        this.catalogoCacheService = catalogoCacheService;
        this.preferenciasService = preferenciasService;
        this.entradasClient = entradasClient;
    }

    public List<RecomendacionResponse> recomendar(String clienteId, int limite) {
        PreferenciasCliente preferencias = preferenciasService.obtener(clienteId);
        List<EntradaHistorialDTO> historial =
                Futures.join(entradasClient.obtenerEntradasPorCliente(clienteId));
        Gustos gustos = construirGustos(preferencias, historial);
        Set<String> eventosYaComprados = eventosYaComprados(historial);

        List<RecomendacionResponse> recomendaciones = new ArrayList<>();
        for (Evento candidato : catalogoCacheService.obtenerPublicados()) {
            if (candidato == null || candidato.getId() == null) {
                continue;
            }
            if (eventosYaComprados.contains(candidato.getId())) {
                continue;
            }

            List<String> razones = new ArrayList<>();
            int puntaje = 0;

            if (candidato.getTipoEvento() != null) {
                if (gustos.tiposExplicitos().containsKey(candidato.getTipoEvento())) {
                    puntaje += 6;
                    razones.add("Tipo favorito");
                }
                if (gustos.tiposHistoricos().containsKey(candidato.getTipoEvento())) {
                    puntaje += 4;
                    razones.add("Según tu historial");
                }
            }

            String artista = buscarArtistaRelevante(candidato, gustos);
            if (artista != null) {
                String artistaNormalizado = normalizar(artista);
                if (contieneTexto(gustos.artistasExplicitos(), artistaNormalizado)) {
                    puntaje += 8;
                    razones.add("Artista favorito: " + artista);
                }
                if (contieneTexto(gustos.artistasHistoricos(), artistaNormalizado)) {
                    puntaje += 4;
                    razones.add("Artistas de tu historial");
                }
            }

            String ciudad = normalizar(candidato.getCiudad());
            if (!ciudad.isEmpty()) {
                if (ciudad.equals(gustos.ciudadExplicita())) {
                    puntaje += 4;
                    razones.add("Tu ciudad");
                }
                if (gustos.ciudadesHistoricas().containsKey(ciudad)) {
                    puntaje += 3;
                    razones.add("Ciudad de tu historial");
                }
            }

            if (puntaje <= 0 || razones.isEmpty()) {
                continue;
            }

            recomendaciones.add(new RecomendacionResponse(
                    candidato,
                    razones.stream().limit(3).collect(Collectors.joining(" · ")),
                    puntaje));
        }

        recomendaciones.sort(
                Comparator.comparingInt(RecomendacionResponse::puntaje).reversed()
                        .thenComparing(
                                recomendacion -> recomendacion.evento().getFechaHora(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(recomendacion -> recomendacion.evento().getNombre(),
                                Comparator.nullsLast(Comparator.naturalOrder())));

        int cantidad = Math.max(1, Math.min(limite, MAX_RECOMENDACIONES));
        return recomendaciones.stream().limit(cantidad).toList();
    }

    private Gustos construirGustos(PreferenciasCliente preferencias, List<EntradaHistorialDTO> historial) {
        Map<TipoEvento, Integer> tiposExplicitos = new LinkedHashMap<>();
        Map<String, Integer> artistasExplicitos = new LinkedHashMap<>();
        for (TipoEvento tipo : preferencias.getTiposEvento() == null
                ? List.<TipoEvento>of() : preferencias.getTiposEvento()) {
            if (tipo != null) {
                tiposExplicitos.put(tipo, 6);
            }
        }
        for (String artista : preferencias.getArtistasFavoritos() == null
                ? List.<String>of() : preferencias.getArtistasFavoritos()) {
            String normalizado = normalizar(artista);
            if (!normalizado.isEmpty()) {
                artistasExplicitos.put(normalizado, 8);
            }
        }

        Map<TipoEvento, Integer> tiposHistoricos = new LinkedHashMap<>();
        Map<String, Integer> artistasHistoricos = new LinkedHashMap<>();
        Map<String, Integer> ciudadesHistoricas = new LinkedHashMap<>();
        for (EntradaHistorialDTO entrada : historial == null ? List.<EntradaHistorialDTO>of() : historial) {
            if (entrada == null || "CANCELADA".equalsIgnoreCase(entrada.estado())) {
                continue;
            }
            int peso = pesoHistorico(entrada.fechaEmision());
            Optional<Evento> evento = Optional.empty();
            if (entrada.eventoId() != null && !entrada.eventoId().isBlank()) {
                evento = catalogoCacheService.obtenerPorId(entrada.eventoId());
            }
            if (evento.isEmpty()) {
                continue;
            }

            Evento comprado = evento.get();
            if (comprado.getTipoEvento() != null) {
                tiposHistoricos.merge(comprado.getTipoEvento(), peso, Integer::sum);
            }
            for (String participante : comprado.getParticipantes() == null
                    ? List.<String>of() : comprado.getParticipantes()) {
                String normalizado = normalizar(participante);
                if (!normalizado.isEmpty()) {
                    artistasHistoricos.merge(normalizado, peso, Integer::sum);
                }
            }
            String ciudad = normalizar(comprado.getCiudad());
            if (!ciudad.isEmpty()) {
                ciudadesHistoricas.merge(ciudad, peso, Integer::sum);
            }
        }

        return new Gustos(
                tiposExplicitos,
                artistasExplicitos,
                normalizar(preferencias.getCiudad()),
                tiposHistoricos,
                artistasHistoricos,
                ciudadesHistoricas);
    }

    private Set<String> eventosYaComprados(List<EntradaHistorialDTO> historial) {
        Set<String> ids = new HashSet<>();
        if (historial == null) {
            return ids;
        }
        for (EntradaHistorialDTO entrada : historial) {
            if (entrada != null && entrada.eventoId() != null
                    && !"CANCELADA".equalsIgnoreCase(entrada.estado())) {
                ids.add(entrada.eventoId());
            }
        }
        return ids;
    }

    private String buscarArtistaRelevante(Evento candidato, Gustos gustos) {
        if (candidato.getParticipantes() == null) {
            return null;
        }
        for (String participante : candidato.getParticipantes()) {
            String normalizado = normalizar(participante);
            if (normalizado.isEmpty()) {
                continue;
            }
            if (contieneTexto(gustos.artistasExplicitos(), normalizado)
                    || contieneTexto(gustos.artistasHistoricos(), normalizado)) {
                return participante;
            }
        }
        return null;
    }

    private boolean contieneTexto(Map<String, Integer> valores, String texto) {
        return valores.containsKey(texto);
    }

    private int pesoHistorico(LocalDateTime fechaEmision) {
        if (fechaEmision == null) {
            return 1;
        }
        long dias = ChronoUnit.DAYS.between(fechaEmision, LocalDateTime.now());
        if (dias < 0 || dias <= 30) {
            return 3;
        }
        if (dias <= 90) {
            return 2;
        }
        return 1;
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }

    private record Gustos(
            Map<TipoEvento, Integer> tiposExplicitos,
            Map<String, Integer> artistasExplicitos,
            String ciudadExplicita,
            Map<TipoEvento, Integer> tiposHistoricos,
            Map<String, Integer> artistasHistoricos,
            Map<String, Integer> ciudadesHistoricas) {}
}
