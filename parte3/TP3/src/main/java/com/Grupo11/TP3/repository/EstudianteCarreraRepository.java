package com.Grupo11.TP3.repository;
import com.Grupo11.TP3.models.EstudianteCarrera;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
//import springboot.app.dtos.ReportePerrosHabilidad;
import springboot.app.modelos.EstudianteCarrera;

@Repository("EstudianteCarreraRepositorio")
public interface EstudianteCarreraRepository extends RepoBase<EstudianteCarrera, EstudianteCarreraPK> {

}