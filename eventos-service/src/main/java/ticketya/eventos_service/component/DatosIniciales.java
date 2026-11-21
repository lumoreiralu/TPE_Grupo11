
package ticketya.eventos_service.component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import ticketya.eventos_service.model.EstadoEvento;
import ticketya.eventos_service.model.Evento;
import ticketya.eventos_service.model.Evento.PrecioSector;
import ticketya.eventos_service.model.Evento.SectorResumen;
import ticketya.eventos_service.model.Lugar;
import ticketya.eventos_service.model.TipoSector;
import ticketya.eventos_service.repository.EventoRepository;
import ticketya.eventos_service.repository.LugarRepository;

@Component
public class DatosIniciales implements ApplicationRunner {

    private final LugarRepository lugarRepository;
    private final EventoRepository eventoRepository;

    public DatosIniciales(
            LugarRepository lugarRepository,
            EventoRepository eventoRepository) {
        this.lugarRepository = lugarRepository;
        this.eventoRepository = eventoRepository;
    }

    @Override
    public void run(ApplicationArguments args) {

        cargarLugares();
        cargarEventos();
    }

    private void cargarLugares() {

        if (lugarRepository.existsById("lugar-teatro")) {
            return;
        }

        Lugar lugar = new Lugar();
        lugar.setId("lugar-teatro");
        lugar.setNombre("Teatro Central");

        lugarRepository.save(lugar);
    }

    private void cargarEventos() {

        if (eventoRepository.existsById("evento-borrador")) {
            return;
        }

        // Evento en borrador
        Evento borrador = new Evento();
        borrador.setId("evento-borrador");
        borrador.setNombre("Evento en borrador");
        borrador.setLugarId("lugar-teatro");
        borrador.setEstado(EstadoEvento.BORRADOR);

        eventoRepository.save(borrador);

        // Evento publicado
        Evento publicado = new Evento();
        publicado.setId("evento-rock");
        publicado.setNombre("Festival Rock");
        publicado.setDescripcion("Festival de música en vivo.");
        publicado.setLugarId("lugar-teatro");
        publicado.setCiudad("Buenos Aires");
        publicado.setFechaHora(LocalDateTime.now().plusDays(30));
        publicado.setEstado(EstadoEvento.PUBLICADO);

        publicado.setSectores(List.of(
            new SectorResumen(
                "sector-general",
                "Campo",
                TipoSector.CAPACIDAD_GENERAL,
                500
            ),
            new SectorResumen(
                "sector-platea",
                "Platea",
                TipoSector.NUMERADO,
                200
            )
        ));

        publicado.setPreciosSectores(List.of(
            new PrecioSector(
                "sector-general",
                new BigDecimal("15000")
            ),
            new PrecioSector(
                "sector-platea",
                new BigDecimal("22000")
            )
        ));

        eventoRepository.save(publicado);
    }
}
