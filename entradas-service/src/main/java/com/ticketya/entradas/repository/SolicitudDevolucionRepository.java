package com.ticketya.entradas.repository;

import com.ticketya.entradas.model.EstadoSolicitudDevolucion;
import com.ticketya.entradas.model.SolicitudDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface SolicitudDevolucionRepository extends JpaRepository<SolicitudDevolucion, UUID>{
    List<SolicitudDevolucion> findByEstado(EstadoSolicitudDevolucion estado);
    Optional<SolicitudDevolucion> findByEntradaIdAndEstado(UUID entradaId, EstadoSolicitudDevolucion estado);
}
