package ticketya.eventos_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.TipoEvento;
import ticketya.eventos_service.repository.EventoRepository;
import ticketya.eventos_service.repository.LugarRepository;
import ticketya.eventos_service.service.EventoBusquedaService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {
    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private EventoBusquedaService busquedaService;

    @PostMapping
    public ResponseEntity<Evento> crearEvento(@RequestBody Evento evento) {
        evento.setEstado(EstadoEvento.BORRADOR);
        completarCiudad(evento);
        Evento nuevoEvento = eventoRepository.save(evento);
        return new ResponseEntity<>(nuevoEvento, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Evento>> obtenerEventos() {
        return new ResponseEntity<>(eventoRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evento> obtenerEventoPorId(@PathVariable String id) {
        return eventoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/publicados")
    public ResponseEntity<List<Evento>> obtenerEventosPublicados() {
        return ResponseEntity.ok(eventoRepository.findByEstado(EstadoEvento.PUBLICADO));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Evento>> buscarEventos(
            @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false) String organizadorId,
            @RequestParam(required = false) String artista,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta) {
        return ResponseEntity.ok(
                busquedaService.buscar(tipo, organizadorId, artista, ciudad, fechaDesde, fechaHasta));
    }

    @PutMapping("/{id}/publicar")
    public ResponseEntity<Evento> publicarEvento(@PathVariable String id) {
        return eventoRepository.findById(id).map(evento ->{
            evento.setEstado(EstadoEvento.PUBLICADO);
            completarCiudad(evento);
            return ResponseEntity.ok(eventoRepository.save(evento));
        }).orElse(ResponseEntity.notFound().build());
    }

    private void completarCiudad(Evento evento) {
        if (evento.getLugarId() != null
                && (evento.getCiudad() == null || evento.getCiudad().isBlank())) {
            lugarRepository.findById(evento.getLugarId())
                    .ifPresent(lugar -> evento.setCiudad(lugar.getCiudad()));
        }
    }

}
