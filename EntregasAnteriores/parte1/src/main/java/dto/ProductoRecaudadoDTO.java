package dto;

public class ProductoRecaudadoDTO {
    private int idProducto;
    private String nombre;
    private float valor;
    private float recaudacion;

    public ProductoRecaudadoDTO(int idProducto, String nombre, float valor, float recaudacion) {
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.valor = valor;
        this.recaudacion = recaudacion;
    }

    public int getIdProducto() {
        return idProducto;
    }
    public String getNombre() {
        return nombre;
    }
    public float getValor() {
        return valor;
    }
    public float getRecaudacion() {
        return recaudacion;
    }
}
