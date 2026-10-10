package com.Grupo11.TP3.repository;

import com.Grupo11.TP3.models.Estudiante;
import java.util.List;


@Repository("EstudianteRepositorio")
public interface EstudianteRepository extends RepoBase<Estudiante, Integer> {

    // Inciso 2.c Método para obtener estudiantes ordenados por apellido ascendentemente
    List<Estudiante> findAllByOrderByApellidoAsc();

    //Inciso 2.d
    @Query("SELECT e FROM Estudiante e WHERE e.lu = :lu")
    Estudiante getEstudianteByLU(int lu);
}
