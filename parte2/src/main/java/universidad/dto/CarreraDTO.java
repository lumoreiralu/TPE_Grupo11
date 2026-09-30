package universidad.dto;

public class CarreraDTO {
    private int id;
    private String nombre;


    public CarreraDTO(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
