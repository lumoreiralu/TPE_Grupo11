package com.Grupo11.TP3.controllers;

import com.Grupo11.TP3.models.Carrera;
import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.models.EstudianteCarrera;
import com.Grupo11.TP3.services.CarreraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/carreras")
public class CarreraController {
    @Autowired
    private CarreraService carreraServicio;


    @PostMapping("")
    public ResponseEntity<?> save(@RequestBody Carrera carrera){
        try{
            return ResponseEntity.status(HttpStatus.OK).body(carreraServicio.save(carrera));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"No se pudo cargar la carrera.\"}");
        }
    }

    // Inciso para ver todas las carreras
    @GetMapping("")
    public ResponseEntity<?> getAllCarreras() {
        try {
            List<Carrera> carreras = carreraServicio.findAll();
            return ResponseEntity.status(HttpStatus.OK).body(carreras);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Error al obtener la lista de carreras.\"}");
        }
    }

    // Inciso 2.h - Reporte de carreras: inscriptos y egresados por año
    // GET /carreras/reporte
    @GetMapping("/reporte")
    public ResponseEntity<?> getReporteCarreras() {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(carreraServicio.getReporteCarreras());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error al generar el reporte de carreras.\"}");
        }
    }



    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Carrera entity) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(carreraServicio.update(id, entity));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo actualizar, revise los campos e intente nuevamente.\"}");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            carreraServicio.delete(id);
            return ResponseEntity.status(HttpStatus.OK).body("{\"mensaje\":\"Registro eliminado correctamente.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo eliminar el registro, verifique el ID e intente nuevamente.\"}");
        }
    }

}