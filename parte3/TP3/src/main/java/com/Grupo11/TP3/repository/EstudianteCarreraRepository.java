package com.Grupo11.TP3.repository;

import com.Grupo11.TP3.models.Carrera;
import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.models.EstudianteCarrera;
import com.Grupo11.TP3.models.EstudianteCarreraPK;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudianteCarreraRepository extends RepoBase<EstudianteCarrera, EstudianteCarreraPK> {

    // Inciso 2.f
    @Query("SELECT ec.carrera " +
            "FROM EstudianteCarrera ec " +
            "GROUP BY ec.carrera " +
            "ORDER BY COUNT(ec.estudiante) DESC")
    List<Carrera> findCarrerasConEstudiantesInscriptosOrdenado();

    // Inciso 2.g
    @Query("SELECT ec.estudiante FROM EstudianteCarrera ec   " +
            "WHERE ec.id.idCarrera = :idCarrera " +
            "AND ec.estudiante.ciudadResidencia = :ciudad")
    List<Estudiante> findEstudiantesPorCarreraYCiudad(
            @Param("idCarrera") Long idCarrera,
            @Param("ciudad") String ciudad
    );
}
