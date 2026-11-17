package com.Grupo11.TP3.models;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EstudianteCarreraPK implements Serializable {
    private Long idEstudiante;
    private Long idCarrera;

    public EstudianteCarreraPK() {}

    public EstudianteCarreraPK(Long idEstudiante, Long idCarrera) {
        this.idEstudiante = idEstudiante;
        this.idCarrera = idCarrera;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EstudianteCarreraPK that = (EstudianteCarreraPK) o;
        return Objects.equals(idEstudiante, that.idEstudiante) && Objects.equals(idCarrera, that.idCarrera);
    }

    @Override
    public int hashCode() {

        return Objects.hash(idEstudiante, idCarrera);
    }

    public Long getIdEstudiante() {
        return idEstudiante;
    }

    public Long getIdCarrera() {
        return idCarrera;
    }
}