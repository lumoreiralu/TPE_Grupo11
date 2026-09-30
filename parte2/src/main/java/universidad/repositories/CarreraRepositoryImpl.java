package universidad.repositories;
import universidad.dto.CarreraInscriptosDTO;
import universidad.dto.ReporteCarreraDTO;
import universidad.entity.Carrera;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.ArrayList;

public class CarreraRepositoryImpl implements CarreraRepository {
    private final EntityManager em;
    public CarreraRepositoryImpl(EntityManager em){
        this.em = em;
    }




    @Override
    public Carrera save(Carrera carrera) {
        if(carrera.getId() == null){
            em.persist(carrera);
        }else{
            carrera = em.merge(carrera);
        }
        return carrera;
    }

    @Override
    public Carrera findById(Integer id) {

        return em.find(Carrera.class, id);
    }

    @Override
    public List<Carrera> findAll() {
        TypedQuery<Carrera> query = em.createQuery("SELECT c FROM Carrera c", Carrera.class);
        return query.getResultList();
    }

    @Override 
    public List<CarreraInscriptosDTO> findAllConInscriptosOrderedByCantInscriptosDesc() {
        String jpql = "SELECT new universidad.dto.CarreraInscriptosDTO(" +
                        "c.nombre, c.id, COUNT(ec)) " +
                        "FROM Carrera c " +
                        "JOIN c.estudiantes ec " +
                        "GROUP BY c.id, c.nombre " +
                        "ORDER BY COUNT(ec) DESC";
        TypedQuery<CarreraInscriptosDTO> query = em.createQuery(jpql, CarreraInscriptosDTO.class);
        return query.getResultList();
    }

    @Override
    public List<ReporteCarreraDTO> getReporteCarreras() {
        String sql =
                "SELECT datos.nombre_carrera, datos.anio, " +
                        "SUM(datos.inscriptos) AS cantidad_inscriptos, " +
                        "SUM(datos.egresados) AS cantidad_egresados " +
                "FROM (" +
                    "SELECT c.nombre AS nombre_carrera, ec.inscripcion AS anio, " +
                            "COUNT(*) AS inscriptos, 0 AS egresados " +
                    "FROM Carrera c " +
                    "JOIN EstudianteCarrera ec ON ec.id_carrera = c.id " +
                    "GROUP BY c.id, c.nombre, ec.inscripcion " +
                    "UNION ALL " +
                    "SELECT c.nombre AS nombre_carrera, ec.graduacion AS anio, " +
                            "0 AS inscriptos, COUNT(*) AS egresados " +
                    "FROM Carrera c " +
                    "JOIN EstudianteCarrera ec ON ec.id_carrera = c.id " +
                    "WHERE ec.graduacion > 0 " +
                    "GROUP BY c.id, c.nombre, ec.graduacion " +
                ") datos " +
                "GROUP BY datos.nombre_carrera, datos.anio " +
                "ORDER BY datos.nombre_carrera ASC, datos.anio ASC";
        List<Object[]> filas = em.createNativeQuery(sql).getResultList();
        List<ReporteCarreraDTO> reporte = new ArrayList<>();
        for (Object[] fila : filas) {
            reporte.add(new ReporteCarreraDTO(
                    (String) fila[0],
                    ((Number) fila[1]).intValue(),
                    ((Number) fila[2]).longValue(),
                    ((Number) fila[3]).longValue()
            ));
        }
        return reporte;
    }

}
