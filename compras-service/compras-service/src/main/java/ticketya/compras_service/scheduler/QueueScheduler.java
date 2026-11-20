package ticketya.compras_service.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ticketya.compras_service.service.FilaVirtualService;

@Component
public class QueueScheduler {

    private static final Logger log = LoggerFactory.getLogger(QueueScheduler.class);

    private final FilaVirtualService filaVirtualService;

    public QueueScheduler(FilaVirtualService filaVirtualService) {
        this.filaVirtualService = filaVirtualService;
    }

    @Scheduled(fixedDelayString = "${compras.fila.promocion-ms:1000}")
    public void promoverTurnos() {
        try {
            filaVirtualService.promoverColas();
        } catch (Exception e) {
            log.warn("No se pudo promover la fila virtual: {}", e.getMessage());
        }
    }
}
