package com.ticketya.entradas.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Component
public class QrCodeGenerator {

    public String generarHashQr(String eventoId, String ordenCompraId, String sectorNombre, String fila, Integer asiento) {
        String data = String.format("%s|%s|%s|%s|%s|%s|%d",
                eventoId,
                ordenCompraId,
                sectorNombre != null ? sectorNombre : "",
                fila != null ? fila : "",
                asiento != null ? asiento : 0,
                UUID.randomUUID(),
                System.nanoTime());

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al generar hash SHA-256 para código QR", e);
        }
    }
}

