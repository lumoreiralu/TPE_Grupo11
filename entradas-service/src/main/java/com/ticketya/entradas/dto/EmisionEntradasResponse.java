package com.ticketya.entradas.dto;

import java.util.List;

public record EmisionEntradasResponse(
        String ordenCompraId,
        List<EntradaResponseDTO> entradas
) {}

