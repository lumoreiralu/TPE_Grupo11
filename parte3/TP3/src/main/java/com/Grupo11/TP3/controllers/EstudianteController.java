package com.Grupo11.TP3.controllers;

import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.services.EstudianteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estudiantes")
public class EstudianteController
{
    @Autowired
    private EstudianteService estudianteServicio;

    // Inciso 2.e
    @GetMapping("/genero/{genero}")
    public ResponseEntity<?> getEstudiantesPorGenero(@PathVariable String genero) {
        try {
            return ResponseEntity.ok(estudianteServicio.getEstudiantesPorGenero(genero));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    //Inciso 2.a
    @PostMapping("")
    public ResponseEntity<?> save(@RequestBody Estudiante estudiante){
        try{
            return ResponseEntity.status(HttpStatus.OK).body(estudianteServicio.save(estudiante));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"No se pudo cargar al estudiante.\"}");
        }
    }

    //inciso 2.d
    @GetMapping("/lu/{lu}")
    public ResponseEntity<?> getEstudianteByLU(@PathVariable int lu){
        try{
            return ResponseEntity.status(HttpStatus.OK).body(estudianteServicio.buscarEstudiantePorLU(lu));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":\"No se encontro el estudiante.\"}");
        }
    }

    // Inciso 2.c
    @GetMapping("/ordenados-por-apellido")
    public ResponseEntity<?> getEstudiantesOrdenados() {
        try {
            List<Estudiante> estudiantes = estudianteServicio.getEstudiantesOrdenadosPorApellido();
            return ResponseEntity.status(HttpStatus.OK).body(estudiantes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Error al obtener la lista de estudiantes.\"}");
        }
    }
}
