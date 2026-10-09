package com.Grupo11.TP3.services;


import com.Grupo11.TP3.repository.CarreraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import universidad.entity.Carrera;

import java.util.List;

@Service("CarreraServicio")
public class CarreraService implements BaseService<Carrera> {

    @Autowired
    private CarreraRepository carreraRepo;


    @Override
    public List<Carrera> findAll() throws Exception {
        return List.of();
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
