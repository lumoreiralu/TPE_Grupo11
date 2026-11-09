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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    public ResponseEntity<?> publicarEvento(
            @PathVariable String id,
            @RequestBody(required = false) List<Evento.PrecioSector> precios) {
        var opt = eventoRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Evento evento = opt.get();
        if (precios != null && !precios.isEmpty()) {
            evento.setPreciosSectores(precios);
        }

        // Validación R2.2: Para publicar, deben existir precios asignados por sector mayores a cero
        if (evento.getPreciosSectores() == null || evento.getPreciosSectores().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("mensaje", "No se puede publicar el evento: debe tener precios definidos por sector"));
        }

        boolean todosConPrecioValido = evento.getPreciosSectores().stream()
                .allMatch(p -> p.getPrecio() != null && p.getPrecio().compareTo(BigDecimal.ZERO) > 0);

        if (!todosConPrecioValido) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("mensaje", "No se puede publicar el evento: todos los sectores deben tener un precio mayor a cero"));
        }

        evento.setEstado(EstadoEvento.PUBLICADO);
        completarCiudad(evento);
        return ResponseEntity.ok(eventoRepository.save(evento));
    }

    private void completarCiudad(Evento evento) {
        if (evento.getLugarId() != null
                && (evento.getCiudad() == null || evento.getCiudad().isBlank())) {
            lugarRepository.findById(evento.getLugarId())
                    .ifPresent(lugar -> evento.setCiudad(lugar.getCiudad()));
        }
    }
}
