package ticketya.eventos_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "preferencias_clientes")
public class PreferenciasCliente implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String clienteId;
    private Set<TipoEvento> tiposEvento = new LinkedHashSet<>();
    private List<String> artistasFavoritos = new ArrayList<>();
    private String ciudad;
    private LocalDateTime fechaActualizacion;
}
