package com.Grupo11.TP3.repository;

import com.Grupo11.TP3.models.EstudianteCarrera;


@Repository("EstudianteRepositorio")
public interface EstudianteRepository extends RepoBase<EstudianteCarrera, Integer> {

    //Inciso 2.d
    @Query("SELECT e FROM Estudiante e WHERE e.lu = :lu")
    Estudiante getEstudianteByLU(int lu);
}
