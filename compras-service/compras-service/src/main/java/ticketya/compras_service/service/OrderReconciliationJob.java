package ticketya.compras_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.time.LocalDateTime;

@Component
public class OrderReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(OrderReconciliationJob.class);

    private final OrdenCompraRepository ordenRepo;
    private final CompraSagaService compraSagaService;

    public OrderReconciliationJob(OrdenCompraRepository ordenRepo, CompraSagaService compraSagaService) {
        this.ordenRepo = ordenRepo;
        this.compraSagaService = compraSagaService;
    }

    @Scheduled(fixedDelay = 30000)
    public void reconciliarOrdenesPendientes() {
        LocalDateTime antiguedadMinima = LocalDateTime.now().minusMinutes(2);
        ordenRepo.findByEstadoAndFechaCreacionBefore(EstadoOrden.PROCESANDO_PAGO, antiguedadMinima)
                .forEach(orden -> {
                    try {
                        compraSagaService.reconciliarOrden(orden.getId());
                    } catch (RuntimeException error) {
                        log.error("No se pudo reconciliar la orden {}; se reintentará en la próxima ejecución",
                                orden.getId(), error);
                    }
                });
    }
}
