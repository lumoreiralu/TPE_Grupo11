package com.Grupo11.TP3.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Id;


import java.util.ArrayList;
import java.util.List;

@Entity
public class Carrera {
    @Id
    private Long id;
    @Column(nullable = false, unique = true)
    private String nombre;
    @Column(nullable = false)
    private int duracion;
    @JsonManagedReference(value = "carrera-estudiante")
    @OneToMany(mappedBy = "carrera", fetch = FetchType.LAZY)
    private List<EstudianteCarrera> estudiantes;
    public Carrera() {
    }

    public Carrera(Long id, String nombre, int duracion) {
        this.id = id;
        this.nombre = nombre;
        this.duracion = duracion;
        this.estudiantes = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {this.id = id; }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getDuracion() {
        return duracion;
    }

    public void setDuracion(int duracion) {
        this.duracion = duracion;
    }

    @Override
    public String toString() {
        return "Carrera{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", duracion=" + duracion +
                '}';
    }
}
