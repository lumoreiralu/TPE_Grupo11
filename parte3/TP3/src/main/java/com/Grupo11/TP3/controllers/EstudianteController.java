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

    //Inciso 2.a
    @PostMapping("")
    public ResponseEntity<?> save(@RequestBody Estudiante estudiante){
        try{
            return ResponseEntity.status(HttpStatus.OK).body(estudianteServicio.save(estudiante));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{Error: No se pudo cargar al estudiante.");

        }
    }

    //inciso 2.d
    @GetMapping("/{lu}")
    public ResponseEntity<?> getEstudianteByLU(int lu){
        try{
            return ResponseEntity.status(HttpStatus.OK).body(estudianteServicio.buscarEstudiantePorLU(libretaUnica));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{Error: intente de nuevo ");
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