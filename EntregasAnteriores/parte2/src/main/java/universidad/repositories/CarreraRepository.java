package universidad.repositories;

import universidad.dto.CarreraInscriptosDTO;
import universidad.dto.ReporteCarreraDTO;
import universidad.entity.Carrera;

import java.util.List;

public interface CarreraRepository {
    Carrera save(Carrera carrera);
    Carrera findById(Integer id);
    List<Carrera> findAll();

    List<CarreraInscriptosDTO> findAllConInscriptosOrderedByCantInscriptosDesc();

    List<ReporteCarreraDTO> getReporteCarreras();

}
