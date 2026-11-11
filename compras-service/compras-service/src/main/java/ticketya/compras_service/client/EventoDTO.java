package ticketya.compras_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoDTO(
        String id,
        String estado,
        List<PrecioSectorDTO> preciosSectores,
        List<SectorInfoDTO> sectores
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PrecioSectorDTO(String idSector, BigDecimal precio) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SectorInfoDTO(String sectorId, String nombre, String tipo, Integer capacidadMaxima) {}
}
