package entity;

import lombok.*;
@AllArgsConstructor
@Getter
@Setter
@ToString
@NoArgsConstructor


public class FacturaProducto {
    private int idFactura;
    private int idProducto;
    private int cantidad;

}