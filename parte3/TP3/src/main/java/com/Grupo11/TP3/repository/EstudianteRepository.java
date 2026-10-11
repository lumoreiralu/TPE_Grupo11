package com.Grupo11.TP3.repository;

import com.Grupo11.TP3.models.Estudiante;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("EstudianteRepositorio")
public interface EstudianteRepository extends RepoBase<Estudiante, Long> {

    // Inciso 2.c Método para obtener estudiantes ordenados por apellido ascendentemente
    List<Estudiante> findAllByOrderByApellidoAsc();

    //Inciso 2.d
    @Query("SELECT e FROM Estudiante e WHERE e.libretaUnica = :lu")
    Estudiante getEstudianteByLU(@Param("lu") int lu);

    //inciso 2.e
    @Query("SELECT e FROM Estudiante e WHERE e.genero = :genero")
    List<Estudiante> findByGenero(@Param("genero") String genero);
}
