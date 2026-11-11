package ticketya.eventos_service.model;

cu
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHora;
    private String lugarId;
    private String ciudad;
    private EstadoEvento estado = EstadoEvento.BORRADOR;
    private List<PrecioSector> preciosSectores;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrecioSector {
        private String idSector;
        private BigDecimal precio;
    }


}
