package com.Grupo11.TP3.repository;

import com.Grupo11.TP3.models.EstudianteCarrera;


@Repository("EstudianteRepositorio")
public interface EstudianteRepository extends RepoBase<Estudiante, Long> {

    //Inciso 2.d
    @Query("SELECT e FROM Estudiante e WHERE e.libretaUnica = :lu")
    Estudiante getEstudianteByLU(@Param("lu") int lu);
}
