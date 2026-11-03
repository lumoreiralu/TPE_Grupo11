package ticketya.eventos_service.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection="eventos")
public class Evento {
    @Id
    private String id;
    private String nombre;
    private String descripcion;
    private TipoEvento tipoEvento;
    private String organizadorId;
    private List<String> participantes;
    private LocalDateTime fechaHora;
    private String lugarId;
    private EstadoEvento estadoEvento = EstadoEvento.BORRADOR;
    private List<PrecioSector> preciosSectores;

    @Data
    public static class PrecioSector {
        private String idSector;
        private BigDecimal precio;
    }
}
