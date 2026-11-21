package ticketya.compras_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import ticketya.compras_service.model.DetalleReserva;
import ticketya.compras_service.model.EstadoOrden;
import ticketya.compras_service.model.OrdenCompra;
import ticketya.compras_service.repository.BloqueoAsientoRepository;
import ticketya.compras_service.repository.CupoSectorRepository;
import ticketya.compras_service.repository.OrdenCompraRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompraTransaccionServiceTest {

    @Mock
    private OrdenCompraRepository ordenRepo;
    @Mock
    private BloqueoAsientoRepository bloqueoRepo;
    @Mock
    private CupoSectorRepository cupoSectorRepo;
    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    void compensarOrdenEliminaBloqueosYRestauraCuposGenerales() {
        UUID ordenId = UUID.randomUUID();
        DetalleReserva asiento = new DetalleReserva();
        asiento.setSectorId("platea");
        asiento.setFila("A");
        asiento.setAsiento(1);
        asiento.setPrecioUnitario(new BigDecimal("100"));
        DetalleReserva cupoGeneral = new DetalleReserva();
        cupoGeneral.setSectorId("campo");
        cupoGeneral.setPrecioUnitario(new BigDecimal("50"));

        OrdenCompra orden = new OrdenCompra();
        orden.setId(ordenId);
        orden.setEventoId("evento-1");
        orden.setEstado(EstadoOrden.PAGO_CONFIRMADO);
        orden.setDetalles(List.of(asiento, cupoGeneral));
        when(ordenRepo.findByIdConDetalles(ordenId)).thenReturn(Optional.of(orden));

        CompraTransaccionService transaccionService = new CompraTransaccionService(
                ordenRepo, bloqueoRepo, cupoSectorRepo, transactionManager);
        transaccionService.registrarOrdenCompensada(ordenId, "falló la emisión");

        verify(bloqueoRepo).deleteByOrdenId(ordenId);
        verify(cupoSectorRepo).incrementarCupo("evento-1", "campo", 1);
        verify(ordenRepo).saveAndFlush(orden);
    }
}
