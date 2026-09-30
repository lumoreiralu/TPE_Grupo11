package universidad.entity;

import jdk.vm.ci.meta.Local;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Estudiante {
    @Id
    private Integer dni;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String apellido;
    @Column(nullable = false)
    private Integer edad;
    @Column (nullable = false)
    private String genero;
    @Column(nullable = false)
    private String ciudadResidencia;
    @Column(name="lu", unique = true, nullable = false)
    private int libretaUnica;
    @OneToMany(mappedBy = "estudiante", fetch = FetchType.LAZY)
    private List<EstudianteCarrera> carreras;

    public Estudiante() {
    }

    public Estudiante(Integer dni, String nombre, String apellido, int edad, String genero, String ciudadResidencia, int libretaUnica) {
        this.dni = dni;
        this.nombre = nombre;
        this.edad = edad; // para leer los csv que ya tiene la edad establecida
        this.apellido = apellido;
        this.genero = genero;
        this.ciudadResidencia = ciudadResidencia;
        this.libretaUnica = libretaUnica;
        this.carreras = new ArrayList<>();
    }

    public Estudiante(Integer dni, String nombre, String apellido, String genero, String ciudadResidencia, int libretaUnica, LocalDate fechaNacimiento) {
        this.dni = dni;
        this.nombre = nombre;
        this.apellido = apellido;
        this.edad = Period.between(fechaNacimiento, LocalDate.now()).getYears(); //buena practica para las nuevas inserciones
        this.genero = genero;
        this.ciudadResidencia = ciudadResidencia;
        this.libretaUnica = libretaUnica;
        this.carreras = new ArrayList<>();

    }

    public Integer getDni() {
        return dni;
    }

    public String getNombre() {
        return nombre;
    }


    public String getApellido() {
        return apellido;
    }


    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public String getCiudadResidencia() {
        return ciudadResidencia;
    }

    public void setCiudadResidencia(String ciudadResidencia) {
        this.ciudadResidencia = ciudadResidencia;
    }

    public int getNum_libreta_uni() {
        return this.libretaUnica;
    }

    public void setLibretaUnica(int num) {
        this.libretaUnica = num;
    }

    public List<EstudianteCarrera> getCarreras() {
        return this.carreras;
    }


    @Override
    public String toString() {
        return "Estudiante{" +
                "dni=" + dni +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", edad=" + edad +
                ", genero='" + genero + '\'' +
                ", ciudadResidencia='" + ciudadResidencia + '\'' +
                ", num_lu=" + libretaUnica +
                '}';
    }
}
