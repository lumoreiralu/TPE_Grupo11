package com.ticketya.entradas.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class QrCodeGeneratorTest {

    private final QrCodeGenerator generator = new QrCodeGenerator();

    @Test
    void generaHashQrValidoYNoNulo() {
        String hash = generator.generarHashQr("evento1", "orden1", "Platea A", "1", 10);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 en hex tiene 64 caracteres
    }

    @Test
    void generaHashesUnicosParaMismosParametros() {
        Set<String> hashes = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String hash = generator.generarHashQr("evento1", "orden1", "Platea A", "1", 10);
            boolean added = hashes.add(hash);
            assertTrue(added, "El hash generado no debe repetirse");
        }
        assertEquals(100, hashes.size());
    }
}

