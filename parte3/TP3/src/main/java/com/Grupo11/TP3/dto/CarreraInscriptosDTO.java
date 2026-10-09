package universidad.dto;
public class CarreraInscriptosDTO {
    private int id;
    private String nombre;
    private long cantidadInscriptos;

    public CarreraInscriptosDTO(String carrera, int id, long cantidadInscriptos) {
        this.id = id;
        this.nombre = carrera;
        this.cantidadInscriptos = cantidadInscriptos;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public long getCantidadInscriptos() {
        return cantidadInscriptos;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setCantidadInscriptos(long cantidadInscriptos) {
        this.cantidadInscriptos = cantidadInscriptos;
    }
    @Override
    public String toString() {
        return "CarreraInscriptosDTO{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", cantidadInscriptos=" + cantidadInscriptos +
                '}';
    }
}
