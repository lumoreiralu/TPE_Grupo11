package com.Grupo11.TP3.services;


import com.Grupo11.TP3.models.Carrera;
import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.repository.CarreraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;

@Service("CarreraServicio")
public class CarreraService implements BaseService<Carrera> {

    @Autowired
    private CarreraRepository carreraRepo;


    @Override
    public List<Carrera> findAll() throws Exception {
        try {
            return carreraRepo.findAll();
        } catch (Exception e) {
            throw new Exception("Error al buscar las carreras: " + e.getMessage());
        }
    }

    @Override
    public Carrera findById(Long id) throws Exception {
        return null;
    }

    @Override
    public Carrera save(Carrera entity) throws Exception {
        return null;
    }

    @Override
    public Carrera update(Long id, Carrera entity) throws Exception {
        return null;
    }

    @Override
    public boolean delete(Long id) throws Exception {
        return false;
    }
}
