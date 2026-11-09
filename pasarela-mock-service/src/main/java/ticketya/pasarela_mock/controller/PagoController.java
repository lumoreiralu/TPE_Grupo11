package ticketya.pasarela_mock.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ticketya.pasarela_mock.dto.PagoRequest;
import ticketya.pasarela_mock.dto.PagoResponse;
import ticketya.pasarela_mock.dto.ReembolsoRequest;
import ticketya.pasarela_mock.dto.ReembolsoResponse;
import ticketya.pasarela_mock.service.PagoService;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @PostMapping("/procesar")
    public ResponseEntity<PagoResponse> procesarPago(@RequestBody PagoRequest request) {
        PagoResponse response = pagoService.procesarPago(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reembolsar")
    public ResponseEntity<ReembolsoResponse> reembolsarPago(@RequestBody ReembolsoRequest request) {
        ReembolsoResponse response = pagoService.reembolsarPago(request);
        return ResponseEntity.ok(response);
    }
}
