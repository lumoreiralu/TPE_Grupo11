package com.Grupo11.TP3.controllers;

import com.Grupo11.TP3.models.Carrera;
import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.services.CarreraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/carreras")
public class CarreraController {
    @Autowired
    private CarreraService carreraServicio;


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

}