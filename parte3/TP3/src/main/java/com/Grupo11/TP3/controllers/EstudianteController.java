package com.Grupo11.TP3.controllers;

import com.Grupo11.TP3.services.EstudianteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estudiantes")
public class EstudianteController
{
    @Autowired
    private EstudianteService estudianteServicio;
}