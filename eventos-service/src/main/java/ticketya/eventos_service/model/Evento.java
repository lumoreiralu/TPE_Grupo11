package ticketya.eventos_service.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection="eventos")
public class Evento implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private ModoFila modoFila = ModoFila.NORMAL;
    private Integer umbralConcurrencia;
    private List<PrecioSector> preciosSectores;
    private List<SectorResumen> sectores;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrecioSector implements Serializable {

        private static final long serialVersionUID = 1L;

        private String idSector;
        private BigDecimal precio;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SectorResumen implements Serializable {

        private static final long serialVersionUID = 1L;

        private String sectorId;
        private String nombre;
        private TipoSector tipo;
        private Integer capacidadMaxima;
    }
}
