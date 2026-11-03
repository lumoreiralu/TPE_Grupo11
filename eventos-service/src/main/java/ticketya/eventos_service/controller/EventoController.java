package ticketya.eventos_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.repository.EventoRepository;

import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {
    @Autowired
    private EventoRepository eventoRepository;

    @PostMapping
    public ResponseEntity<Evento> crearEvento(@RequestBody Evento evento) {
        evento.setEstadoEvento(EstadoEvento.BORRADOR);
        Evento nuevoEvento = eventoRepository.save(evento);
        return new ResponseEntity<>(nuevoEvento, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Evento>> obtenerEventos() {
        return new ResponseEntity<>(eventoRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping("/publicados")
    public ResponseEntity<List<Evento>> obtenerEventosPublicados() {
        return ResponseEntity.ok(eventoRepository.findByEstado(EstadoEvento.PUBLICADO));
    }

    @PutMapping("/{id}/publicar")
    public ResponseEntity<Evento> publicarEvento(@PathVariable String id) {
        return eventoRepository.findById(id).map(evento ->{
            evento.setEstadoEvento(EstadoEvento.PUBLICADO);
            return ResponseEntity.ok(eventoRepository.save(evento));
        }).orElse(ResponseEntity.notFound().build());
    }

}
