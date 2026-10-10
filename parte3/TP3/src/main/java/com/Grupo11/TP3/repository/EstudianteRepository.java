package com.Grupo11.TP3.repository;

import com.Grupo11.TP3.models.Estudiante;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstudianteRepository extends RepoBase<Estudiante, Long> { // Cambiado a Estudiante y Long (por el DNI)

    // Inciso 2.d (Corregido: e.libretaUnica y agregado @Param)
    @Query("SELECT e FROM Estudiante e WHERE e.libretaUnica = :lu")
    Estudiante getEstudianteByLU(@Param("lu") int lu);

    // Inciso 2.c Método para obtener estudiantes ordenados por apellido ascendentemente
    List<Estudiante> findAllByOrderByApellidoAsc();
}