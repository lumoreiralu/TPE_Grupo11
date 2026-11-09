package ticketya.pasarela_mock.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import ticketya.pasarela_mock.dto.PagoRequest;
import ticketya.pasarela_mock.dto.PagoResponse;
import ticketya.pasarela_mock.dto.ReembolsoRequest;
import ticketya.pasarela_mock.dto.ReembolsoResponse;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PagoServiceTest {

    private final PagoService pagoService = new PagoService();

    @Test
    void apruebaPagoValido() {
        PagoRequest request = new PagoRequest(new BigDecimal("150.00"), "orden-1", "TARJETA");
        PagoResponse response = pagoService.procesarPago(request);

        assertNotNull(response);
        assertEquals("APROBADO", response.getEstado());
        assertNotNull(response.getTransaccionId());
    }

    @Test
    void rechazaPagoConMontoInvalido() {
        PagoRequest requestCero = new PagoRequest(BigDecimal.ZERO, "orden-2", "TARJETA");
        PagoResponse responseCero = pagoService.procesarPago(requestCero);
        assertEquals("RECHAZADO", responseCero.getEstado());

        PagoRequest requestNegativo = new PagoRequest(new BigDecimal("-10.00"), "orden-3", "TARJETA");
        PagoResponse responseNegativo = pagoService.procesarPago(requestNegativo);
        assertEquals("RECHAZADO", responseNegativo.getEstado());
    }

    @Test
    void rechazaPagoPorMetodoRechazadoOFondosInsuficientes() {
        PagoRequest request1 = new PagoRequest(new BigDecimal("100.00"), "orden-4", "TARJETA_RECHAZADA");
        PagoResponse response1 = pagoService.procesarPago(request1);
        assertEquals("RECHAZADO", response1.getEstado());
        assertTrue(response1.getMensaje().contains("fondos"));

        PagoRequest request2 = new PagoRequest(new BigDecimal("100.00"), "orden-5", "FONDOS_INSUFICIENTES");
        PagoResponse response2 = pagoService.procesarPago(request2);
        assertEquals("RECHAZADO", response2.getEstado());
    }

    @Test
    void rechazaPagoPorTarjetaVencida() {
        PagoRequest request = new PagoRequest(new BigDecimal("100.00"), "orden-6", "TARJETA_VENCIDA");
        PagoResponse response = pagoService.procesarPago(request);
        assertEquals("RECHAZADO", response.getEstado());
        assertTrue(response.getMensaje().contains("vencida") || response.getMensaje().contains("expiración"));
    }

    @Test
    void simulaErrorTecnico500() {
        PagoRequest request = new PagoRequest(new BigDecimal("100.00"), "orden-7", "ERROR_TECNICO");
        assertThrows(ResponseStatusException.class, () -> pagoService.procesarPago(request));
    }

    @Test
    void procesaReembolsoExitoso() {
        ReembolsoRequest request = new ReembolsoRequest("txn-123", new BigDecimal("100.00"));
        ReembolsoResponse response = pagoService.reembolsarPago(request);
        assertNotNull(response);
        assertEquals("REEMBOLSADO", response.getEstado());
        assertNotNull(response.getReembolsoId());
    }
}
