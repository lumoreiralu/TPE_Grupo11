package ticketya.eventos_service.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "lugares")
public class Lugar {
    @Id
    private String id;
    private String nombre;
    private String direccion;
    private Ubicacion ubicacion;
    private List<Sector> sectores;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ubicacion {
        private double latitud;
        private double longitud;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Sector {
        private String sectorId;
        private String nombre;
        private TipoSector tipo;
        private Integer capacidadMaxima;
        private List<FilaSector> filas;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilaSector {
        private String fila;
        private List<Integer> asientos;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setSectores(List<Sector> sectores) {
        this.sectores = sectores;
    }
}
