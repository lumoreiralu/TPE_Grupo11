package entity;

import lombok.*;
//La responsabilidad es representar los datos de un cliente.

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Cliente {
    private int id;
    private String nombre;
    private String email;

}