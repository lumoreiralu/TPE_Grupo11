package com.ticketya.entradas.repository;

import com.ticketya.entradas.model.Entrada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntradaRepository extends JpaRepository<Entrada, UUID> {

    List<Entrada> findByOrdenCompraId(String ordenCompraId);

    Optional<Entrada> findByCodigoQrHash(String codigoQrHash);

    List<Entrada> findByClienteId(String clienteId);
}
