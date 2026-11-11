package ticketya.compras_service.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ticketya.compras_service.service.ReservaService;

@Component
public class LiberadorReservas {

    private final ReservaService reservaService;

    public LiberadorReservas(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // Cada 30 segundos busca reservas vencidas y libera sus asientos
    @Scheduled(fixedDelay = 30000)
    public void liberar() {
        reservaService.liberarReservasVencidas();
    }
}