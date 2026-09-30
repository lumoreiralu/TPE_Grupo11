package entity;

import lombok.*;
@AllArgsConstructor
@Getter
@Setter
@ToString
@NoArgsConstructor


//La responsabilidad es representar los datos de un producto.


public class Producto {
    private int idProducto;
    private String nombre;
    private float valor;

}