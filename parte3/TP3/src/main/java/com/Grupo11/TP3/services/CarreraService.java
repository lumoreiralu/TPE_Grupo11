package com.Grupo11.TP3.services;


import com.Grupo11.TP3.dto.ReporteCarreraDTO;
import com.Grupo11.TP3.models.Carrera;
import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.repository.CarreraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Service("CarreraServicio")
public class CarreraService implements BaseService<Carrera> {

    @Autowired
    private CarreraRepository carreraRepo;


    @Override
    public List<Carrera> findAll() throws Exception {
        try {
            return carreraRepo.findAll();
        } catch (Exception e) {
            throw new Exception("Error al buscar las carreras: " + e.getMessage());
        }
    }

    @Override
    public Carrera findById(Long id) throws Exception {
        return null;
    }

    @Override
    public Carrera save(Carrera entity) throws Exception {
        return null;
    }

    @Override
    public Carrera update(Long id, Carrera entity) throws Exception {
        return null;
    }

    @Override
    public boolean delete(Long id) throws Exception {
        return false;
    }

    // Inciso 2.h - Reporte de carreras: inscriptos y egresados por año.
    // Se hacen 2 consultas JPQL (inscriptos y egresados) y se combinan acá, porque
    // un mismo año puede tener inscriptos pero no egresados (o al revés).
    @Transactional(readOnly = true)
    public List<ReporteCarreraDTO> getReporteCarreras() throws Exception {
        try {
            // carrera -> (año -> [inscriptos, egresados]).
            // Los TreeMap ordenan solos: carreras alfabéticamente (Collator en español,
            // ignora mayúsculas/acentos) y años de menor a mayor (cronológico).
            Map<String, Map<Integer, long[]>> reporte =
                    new TreeMap<>(Collator.getInstance(Locale.forLanguageTag("es")));

            for (Object[] fila : carreraRepo.contarInscriptosPorAnio()) {
                String carrera = (String) fila[0];
                int anio = ((Number) fila[1]).intValue();
                long cantidad = ((Number) fila[2]).longValue();
                reporte.computeIfAbsent(carrera, k -> new TreeMap<>())
                        .computeIfAbsent(anio, k -> new long[2])[0] = cantidad;
            }

            for (Object[] fila : carreraRepo.contarEgresadosPorAnio()) {
                String carrera = (String) fila[0];
                int anio = ((Number) fila[1]).intValue();
                long cantidad = ((Number) fila[2]).longValue();
                reporte.computeIfAbsent(carrera, k -> new TreeMap<>())
                        .computeIfAbsent(anio, k -> new long[2])[1] = cantidad;
            }

            List<ReporteCarreraDTO> resultado = new ArrayList<>();
            for (Map.Entry<String, Map<Integer, long[]>> carrera : reporte.entrySet()) {
                for (Map.Entry<Integer, long[]> anio : carrera.getValue().entrySet()) {
                    long[] cantidades = anio.getValue();
                    resultado.add(new ReporteCarreraDTO(
                            carrera.getKey(), anio.getKey(), cantidades[0], cantidades[1]));
                }
            }
            return resultado;
        } catch (Exception e) {
            throw new Exception("Error al generar el reporte de carreras: " + e.getMessage());
        }
    }
}