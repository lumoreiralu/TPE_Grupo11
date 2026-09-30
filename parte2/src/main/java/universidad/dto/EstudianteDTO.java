package universidad.dto;

public class EstudianteDTO {

    private Integer dni;
    private String nombre;
    private String genero;
    private int lu;


    public EstudianteDTO(int dni, String genero, String nombre, int lu) {
        this.dni = dni;
        this.genero = genero;
        this.nombre = nombre;
        this.lu = lu;
    }

    public String getGenero() {
        return this.genero;
    }

    public int getDni() {
        return this.dni;
    }

    public String getNombre() {
        return this.nombre;
    }

    public int getLu() {
        return this.lu;
    }
}
