package com.Grupo11.TP3.services;
package springboot.app.servicios;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
//import springboot.app.dtos.ReportePerrosHabilidad;
//import springboot.app.dtos.PerroDTO;
import springboot.app.modelos.EstudianteCarrera;
import springboot.app.repositorios.EstudianteCarreraRepositorio;


import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EstudianteCarreraService implements BaseService<EstudianteCarrera> {

    //2.b)
    @Override
    @Transactional
    public EstudianteCarrera save(EstudianteCarrera entity) throws Exception {
        try{
            return EstudianteCarreraRepositorio.save(entity);
        }catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }
}
