package ticketya.compras_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.model.OrdenCompra;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderReconciliationJobTest {

    @Mock
    private OrdenCompraRepository ordenRepo;
    @Mock
    private CompraSagaService compraSagaService;
    @InjectMocks
    private OrderReconciliationJob job;

    @Test
    void reconciliaOrdenesProcesandoPagoConMasDeDosMinutos() {
        UUID ordenId = UUID.randomUUID();
        OrdenCompra pendiente = new OrdenCompra();
        pendiente.setId(ordenId);
        when(ordenRepo.findByEstadoAndFechaCreacionBefore(
                eq(EstadoOrden.PROCESANDO_PAGO),
                argThat(fecha -> !fecha.isAfter(LocalDateTime.now().minusMinutes(2)))))
                .thenReturn(List.of(pendiente));

        job.reconciliarOrdenesPendientes();

        verify(compraSagaService).reconciliarOrden(ordenId);
    }
}
