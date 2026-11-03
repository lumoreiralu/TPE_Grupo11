package ticketya.eventos_service.model;


import lombok.Data;
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
    private List<Sector> sectores;

    @Data
    public static class Ubicacion {
        private double latitud;
        private double longitud;
    }

    @Data
    public static class Sector {
        private String sectorId;
        private String nombre;
        private TipoSector tipo;
        private Integer capacidadMaxima;
        private List<FilaSector> filas;
    }

    @Data
    public static class FilaSector {
        private String fila;
        private List<Integer> asientos;
    }
}
