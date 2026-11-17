package com.Grupo11.TP3.repository;

import com.Grupo11.TP3.models.Carrera;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarreraRepository extends RepoBase<Carrera, Long> {

    // Inciso 2.h (paso 1): cantidad de inscriptos por carrera y por año de inscripción.
    // Devuelve filas [nombreCarrera, anio, cantidad].
    @Query("SELECT c.nombre, ec.inscripcion, COUNT(ec.estudiante) " +
            "FROM Carrera c JOIN c.estudiantes ec " +
            "GROUP BY c.nombre, ec.inscripcion")
    List<Object[]> contarInscriptosPorAnio();

    // Inciso 2.h (paso 2): cantidad de egresados por carrera y por año de graduación.
    // graduacion = 0 o NULL significa "todavía no se graduó", por eso se filtra.
    // Devuelve filas [nombreCarrera, anio, cantidad].
    @Query("SELECT c.nombre, ec.graduacion, COUNT(ec.estudiante) " +
            "FROM Carrera c JOIN c.estudiantes ec " +
            "WHERE ec.graduacion IS NOT NULL AND ec.graduacion > 0 " +
            "GROUP BY c.nombre, ec.graduacion")
    List<Object[]> contarEgresadosPorAnio();
}
