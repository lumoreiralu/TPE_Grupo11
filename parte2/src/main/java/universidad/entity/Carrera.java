package universidad.entity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Carrera {
    @Id
    private Integer id;
    @Column(nullable = false, unique = true)
    private String nombre;
    @Column(nullable = false)
    private int duracion;
    @OneToMany(mappedBy = "carrera", fetch = FetchType.LAZY)
    private List<EstudianteCarrera> estudiantes;
    public Carrera() {
    }

    public Carrera(Integer id, String nombre, int duracion) {
        this.id = id;
        this.nombre = nombre;
        this.duracion = duracion;
        this.estudiantes = new ArrayList<>();
    }

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {this.id = id; }

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
