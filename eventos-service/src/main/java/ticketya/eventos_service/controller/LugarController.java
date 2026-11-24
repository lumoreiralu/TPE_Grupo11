package ticketya.eventos_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticketya.eventos_service.model.Lugar;
import ticketya.eventos_service.model.TipoSector;
import ticketya.eventos_service.repository.LugarRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/lugares")
public class LugarController {
    @Autowired
    private LugarRepository lugarRepository;

    @PostMapping
    public ResponseEntity<?> crearLugar(@RequestBody Lugar lugar){
        if (lugar.getNombre() == null || lugar.getNombre().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("mensaje", "El nombre del lugar es obligatorio"));
        }
        String errorValidacion = validarSectores(lugar.getSectores());
        if (errorValidacion != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", errorValidacion));
        }
        normalizarSectores(lugar.getSectores());

        Lugar nuevoLugar = lugarRepository.save(lugar);
        return new ResponseEntity<>(nuevoLugar, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Lugar>> obtenerLugares(){
        return new ResponseEntity<>(lugarRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lugar> obtenerLugarPorId(@PathVariable String id){
        return lugarRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/sectores")
    public ResponseEntity<?> agregarSector(@PathVariable String id, @RequestBody Lugar.Sector nuevoSector) {
        var opt = lugarRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Lugar lugar = opt.get();
        if (lugar.getSectores() == null) {
            lugar.setSectores(new ArrayList<>());
        }

        String errorValidacion = validarSector(nuevoSector);
        if (errorValidacion != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", errorValidacion));
        }

        lugar.getSectores().add(nuevoSector);
        return ResponseEntity.ok(lugarRepository.save(lugar));
    }

    private String validarSectores(List<Lugar.Sector> sectores) {
        if (sectores == null || sectores.isEmpty()) {
            return "El lugar debe tener al menos un sector";
        }
        Set<String> ids = new HashSet<>();
        for (Lugar.Sector sector : sectores) {
            if (sector == null) {
                return "Los sectores no pueden estar vacíos";
            }
            if (sector.getNombre() == null || sector.getNombre().isBlank()) {
                return "Cada sector debe tener un nombre";
            }
            if (sector.getSectorId() != null && !sector.getSectorId().isBlank()) {
                String id = sector.getSectorId().trim();
                if (!ids.add(id)) {
                    return "Los identificadores de sector deben ser únicos";
                }
            }
            String err = validarSector(sector);
            if (err != null) return err;
        }
        return null;
    }

    private void normalizarSectores(List<Lugar.Sector> sectores) {
        if (sectores == null) return;
        for (Lugar.Sector sector : sectores) {
            if (sector.getSectorId() == null || sector.getSectorId().isBlank()) {
                sector.setSectorId(UUID.randomUUID().toString());
            }
            if (sector.getTipo() == TipoSector.NUMERADO) {
                sector.setCapacidadMaxima(null);
            } else {
                sector.setFilas(null);
            }
        }
    }

    private String validarSector(Lugar.Sector sector) {
        if (sector.getTipo() == null) {
            return "Cada sector debe especificar su tipo (NUMERADO o CAPACIDAD_GENERAL)";
        }
        // R1.2: Sector numerado exige filas y asientos
        if (sector.getTipo() == TipoSector.NUMERADO) {
            if (sector.getFilas() == null || sector.getFilas().isEmpty()) {
                return "El sector numerado '" + sector.getNombre() + "' debe especificar filas y asientos";
            }
            for (Lugar.FilaSector fila : sector.getFilas()) {
                if (fila == null) {
                    return "Cada fila de un sector numerado debe tener nombre de fila y lista de asientos";
                }
                if (fila.getFila() == null || fila.getFila().isBlank() || fila.getAsientos() == null || fila.getAsientos().isEmpty()) {
                    return "Cada fila de un sector numerado debe tener nombre de fila y lista de asientos";
                }
                Set<Integer> asientos = new HashSet<>();
                for (Integer asiento : fila.getAsientos()) {
                    if (asiento == null || asiento <= 0) {
                        return "Los asientos del sector numerado '" + sector.getNombre() + "' deben ser números mayores a cero";
                    }
                    if (!asientos.add(asiento)) {
                        return "Los asientos de una fila deben ser únicos";
                    }
                }
            }
        }
        // R1.3: Sector no numerado exige capacidad máxima permitida
        if (sector.getTipo() == TipoSector.CAPACIDAD_GENERAL) {
            if (sector.getCapacidadMaxima() == null || sector.getCapacidadMaxima() <= 0) {
                return "El sector general '" + sector.getNombre() + "' debe especificar una capacidad máxima permitida mayor a cero";
            }
        }
        return null;
    }
}
