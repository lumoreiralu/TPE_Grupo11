package com.ticketya.entradas.dto;

import java.util.List;

public record EmisionEntradasRequest(
        String ordenCompraId,
        String clienteId,
        String eventoId,
        List<ItemEntradaEmision> asientos
) {}

