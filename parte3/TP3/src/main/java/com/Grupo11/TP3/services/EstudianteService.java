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
        return estudianteRepository.findAll();
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
    public List<Estudiante> getEstudiantesOrdenadosPorApellido() throws Exception {
        try {
            return estudianteRepository.findAllByOrderByApellidoAsc();
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    // Inciso 2.e
    @Transactional
    public List<Estudiante> getEstudiantesPorGenero(String genero) throws Exception {
        try {
            return estudianteRepository.findByGenero(genero);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }
    }

    @Override
    @Transactional
    public Estudiante update(Long id, Estudiante entity) throws Exception {
        try {
            if (!estudianteRepository.existsById(id)) {
                throw new Exception("No se encontró el estudiante con el ID: " + id);
            }
            entity.setDni(id);

            // save funciona como update si el ID ya existe en la base de datos
            return estudianteRepository.save(entity);
        } catch (Exception e) {
            throw new Exception("Error al actualizar el estudiante: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean delete(Long id) throws Exception {
        try {
            if (!estudianteRepository.existsById(id)) {
                throw new Exception("No se encontró el estudiante con el ID: " + id);
            }

            estudianteRepository.deleteById(id);
            return true; // Retornamos true si se borró con éxito
        } catch (Exception e) {
            throw new Exception("Error al eliminar el estudiante: " + e.getMessage(), e);
        }
    }
}
