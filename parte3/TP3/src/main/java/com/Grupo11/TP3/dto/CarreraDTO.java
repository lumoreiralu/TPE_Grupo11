package com.Grupo11.TP3.dto;

public class CarreraDTO {
    private Long id;
    private String nombre;


    public CarreraDTO(Long id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
