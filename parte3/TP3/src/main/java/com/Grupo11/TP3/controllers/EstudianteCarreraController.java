package com.Grupo11.TP3.controllers;

import com.Grupo11.TP3.models.EstudianteCarrera;
import com.Grupo11.TP3.services.EstudianteCarreraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/estudiantescarreras")
public class EstudianteCarreraController {
    @Autowired
    private EstudianteCarreraService estudianteCarreraServicio;

    //Inciso 2.b
    @PostMapping("")
    public ResponseEntity<?> save(@RequestBody EstudianteCarrera entity) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(estudianteCarreraServicio.save(entity));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo ingresar, revise los campos e intente nuevamente.\"}");
        }
    }

    //Inciso 2.f
    @GetMapping("/con-estudiantes")
    public ResponseEntity<?> findCarrerasConEstudiantesInscriptosOrdenado() {
        try {
            return ResponseEntity.ok(estudianteCarreraServicio.findCarrerasConEstudiantesInscriptosOrdenado());
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    //Inciso 2.g
    @GetMapping("/id/{id}?ciudad={ciudad}")
    public ResponseEntity<?> findEstudiantesPorCarreraYCiudad(@PathVariable Long id, @RequestParam String ciudad) {
        try {
            return ResponseEntity.ok(estudianteCarreraServicio.findEstudiantesPorCarreraYCiudad(id, ciudad));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
