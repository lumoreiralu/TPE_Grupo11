package com.ticketya.entradas.service;

import com.ticketya.entradas.dto.EmisionEntradasRequest;
import com.ticketya.entradas.dto.EmisionEntradasResponse;
import com.ticketya.entradas.dto.ItemEntradaEmision;
import com.ticketya.entradas.model.EstadoEntrada;
import com.ticketya.entradas.repository.EntradaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EntradaServiceTest {

    @Autowired
    private EntradaService entradaService;

    @Autowired
    private EntradaRepository entradaRepository;

    @BeforeEach
    void setUp() {
        entradaRepository.deleteAll();
    }

    @Test
    void emiteEntradasConQrUnico() {
        EmisionEntradasRequest request = new EmisionEntradasRequest(
                "orden-123",
                "cliente-456",
                "evento-789",
                List.of(
                        new ItemEntradaEmision("Platea", "A", 1),
                        new ItemEntradaEmision("Platea", "A", 2)
                )
        );

        EmisionEntradasResponse response = entradaService.emitirEntradas(request);

        assertNotNull(response);
        assertEquals("orden-123", response.ordenCompraId());
        assertEquals(2, response.entradas().size());

        response.entradas().forEach(entrada -> {
            assertNotNull(entrada.id());
            assertNotNull(entrada.codigoQrHash());
            assertEquals(64, entrada.codigoQrHash().length());
            assertEquals(EstadoEntrada.EMITIDA, entrada.estado());
            assertEquals("evento-789", entrada.eventoId());
            assertEquals("cliente-456", entrada.clienteId());
            assertNotNull(entrada.fechaEmision());
        });

        // Aseguramos que los hashes sean diferentes entre asientos
        assertNotEquals(
                response.entradas().get(0).codigoQrHash(),
                response.entradas().get(1).codigoQrHash()
        );
    }

    @Test
    void emisionEsIdempotentePorOrden() {
        EmisionEntradasRequest request = new EmisionEntradasRequest(
                "orden-idempotente",
                "cliente-1",
                "evento-1",
                List.of(new ItemEntradaEmision("General", null, null))
        );

        EmisionEntradasResponse primeraEmision = entradaService.emitirEntradas(request);
        EmisionEntradasResponse segundaEmision = entradaService.emitirEntradas(request);

        assertEquals(primeraEmision.entradas().size(), segundaEmision.entradas().size());
        assertEquals(primeraEmision.entradas().get(0).id(), segundaEmision.entradas().get(0).id());
        assertEquals(primeraEmision.entradas().get(0).codigoQrHash(), segundaEmision.entradas().get(0).codigoQrHash());
    }

    @Test
    void obtenerPorClienteDevuelveSoloSusEntradas() {
        entradaService.emitirEntradas(new EmisionEntradasRequest(
                "orden-a",
                "cliente-a",
                "evento-1",
                List.of(new ItemEntradaEmision("Platea", "A", 1))
        ));
        entradaService.emitirEntradas(new EmisionEntradasRequest(
                "orden-b",
                "cliente-b",
                "evento-1",
                List.of(new ItemEntradaEmision("Platea", "A", 2))
        ));

        var entradas = entradaService.obtenerPorCliente("cliente-a");

        assertEquals(1, entradas.size());
        assertEquals("cliente-a", entradas.get(0).clienteId());
        assertEquals("orden-a", entradas.get(0).ordenCompraId());
    }
}

