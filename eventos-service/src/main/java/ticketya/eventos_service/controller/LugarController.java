package ticketya.eventos_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticketya.eventos_service.model.Lugar;
import ticketya.eventos_service.repository.LugarRepository;

import java.util.List;

@RestController
@RequestMapping("/api/lugares")
public class LugarController {
    @Autowired
    private LugarRepository lugarRepository;

    @PostMapping
    public ResponseEntity<Lugar> crearLugar(@RequestBody Lugar lugar){
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
}
