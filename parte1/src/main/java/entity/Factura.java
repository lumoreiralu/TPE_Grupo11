package entity;

import lombok.*;
@AllArgsConstructor
@Getter
@Setter
@ToString
@NoArgsConstructor

//La responsabilidad es representar los datos de una factura .
public class Factura {
    private int idFactura;
    private int idCliente;

}