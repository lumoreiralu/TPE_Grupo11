package com.Grupo11.TP3.controllers;

import com.Grupo11.TP3.services.EstudianteCarreraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estudiantsecarreras")
public class EstudianteCarreraController {
    @Autowired
    private EstudianteCarreraService estudianteCarreraServicio;


}