package com.Grupo11.TP3.services;


import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.repository.EstudianteRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("EstudianteServicio")
public class EstudianteService implements BaseService<Estudiante>{

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Override
    public List<Estudiante> findAll() throws Exception {
        return List.of();
    }

    @Override
    public Estudiante findById(Long id) throws Exception {
        return null;
    }

    //Inciso 2.a
    @Override
    @Transactional
    public Estudiante save(Estudiante estudiante) throws Exception{
        try{
            return  estudianteRepository.save(estudiante);
        }
        catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public Estudiante update(Long id, Estudiante entity) throws Exception {
        return null;
    }

    @Override
    public boolean delete(Long id) throws Exception {
        return false;
    }

    //Inciso 2.d
    @Transactional
    public Estudiante buscarEstudiantePorLU(int lu) throws Exception{
        try{
            return estudianteRepository.getEstudianteByLU(lu);
        }catch(Exception e){
            throw new Exception(e.getMessage());
        }
    }

    // Inciso 2.c
    @Transactional
    public List<Estudiante> getEstudiantesOrdenadosPorApellido() {
        return estudianteRepository.findAllByOrderByApellidoAsc();
    }

}
