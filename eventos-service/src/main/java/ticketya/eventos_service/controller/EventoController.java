package ticketya.eventos_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ticketya.eventos_service.client.EntradasClient;
import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.TipoEvento;
import ticketya.eventos_service.repository.EventoRepository;
import ticketya.eventos_service.service.CatalogoCacheService;
import ticketya.eventos_service.util.Futures;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {
    private static final Logger log = LoggerFactory.getLogger(EventoController.class);

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private CatalogoCacheService catalogoCacheService;

    @Autowired
    private EntradasClient entradasClient;

    @PostMapping
    public ResponseEntity<Evento> crearEvento(@RequestBody Evento evento) {
        evento.setEstado(EstadoEvento.BORRADOR);
        catalogoCacheService.completarDatosLugar(evento);
        Evento nuevoEvento = eventoRepository.save(evento);
        return new ResponseEntity<>(nuevoEvento, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Evento>> obtenerEventos() {
        List<Evento> eventos = eventoRepository.findAll();
        eventos.forEach(catalogoCacheService::completarDatosLugar);
        return new ResponseEntity<>(eventos, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Evento> obtenerEventoPorId(@PathVariable String id) {
        return ResponseEntity.of(catalogoCacheService.obtenerPorId(id));
    }

    @GetMapping("/publicados")
    public ResponseEntity<List<Evento>> obtenerEventosPublicados() {
        return ResponseEntity.ok(catalogoCacheService.obtenerPublicados());
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Evento>> buscarEventos(
            @RequestParam(required = false) TipoEvento tipo,
            @RequestParam(required = false) String organizadorId,
            @RequestParam(required = false) String artista,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false, defaultValue = "fecha")
String ordenarPor) {
        List<Evento> resultado = catalogoCacheService.buscar(
                tipo,
                organizadorId,
                artista,
                ciudad,
                fechaDesde,
                fechaHasta,
                ordenarPor
        );
        return ResponseEntity.ok(resultado);
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
        catalogoCacheService.completarDatosLugar(evento);
        Evento guardado = eventoRepository.save(evento);
        catalogoCacheService.invalidarCatalogo();
        return ResponseEntity.ok(guardado);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<String> cancelarEvento(@PathVariable String id){
        //1. cambiar estado en MongoDB
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
        evento.setEstado(EstadoEvento.CANCELADO);
        eventoRepository.save(evento);
        catalogoCacheService.invalidarCatalogo();

        //2. Notificar a entradas-service para cancelar entradas y liberar asientos
        try{
            Futures.join(entradasClient.cancelarEntradasPorEvento(id));
        } catch(Exception e){
            log.error("Aviso al notificar a entradas-service para el evento {}: {}", id, e.getMessage());
        }
        return ResponseEntity.ok("Evento cancelado y proceso de reintegros disparado");
    }
}
