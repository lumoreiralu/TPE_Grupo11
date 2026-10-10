package com.Grupo11.TP3.services;
import com.Grupo11.TP3.models.EstudianteCarrera;
import com.Grupo11.TP3.repository.EstudianteCarreraRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

@Service("EstudianteCarreraService")
public class EstudianteCarreraService implements BaseService<EstudianteCarrera> {

    @Autowired // <--- 2. Inyectamos la instancia del repositorio
    private EstudianteCarreraRepository estudianteCarreraRepository;

    @Override
    public List<EstudianteCarrera> findAll() throws Exception {
        return List.of();
    }

    @Override
    public EstudianteCarrera findById(Long id) throws Exception {
        return null;
    }

    //2.b)
    @Override
    @Transactional
    public EstudianteCarrera save(EstudianteCarrera entity) throws Exception {
        try{
            return estudianteCarreraRepository.save(entity);
        }catch (Exception e){
            throw new Exception(e.getMessage());
        }
    }

    @Override
    public EstudianteCarrera update(Long id, EstudianteCarrera entity) throws Exception {
        return null;
    }

    @Override
    public boolean delete(Long id) throws Exception {
        return false;
    }
}
