package universidad.repositories;

import universidad.entity.Carrera;
import universidad.entity.Estudiante;
import universidad.entity.EstudianteCarrera;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;

public class EstudianteCarreraRepositoryImpl implements EstudianteCarreraRepository {
    private final EntityManager em;

    public EstudianteCarreraRepositoryImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public EstudianteCarrera matricular(Estudiante estudiante, Carrera carrera, Integer graduacion) {
        Estudiante eManaged = em.find(Estudiante.class, estudiante.getDni());
        Carrera cManaged = em.find(Carrera.class, carrera.getId());
        if (eManaged == null || cManaged == null) {
            throw new IllegalArgumentException("El estudiante o la carrera no existen en la base de datos.");
        }

        EstudianteCarrera ec = new EstudianteCarrera(cManaged, eManaged, graduacion);
        em.persist(ec);
        return ec;
    }

    @Override
    public EstudianteCarrera save(EstudianteCarrera estudianteCarrera) {
        if(estudianteCarrera.getId() == null){
            em.persist(estudianteCarrera);
        }else{
            estudianteCarrera = em.merge(estudianteCarrera);
        }
        return estudianteCarrera;
    }

    @Override
    public EstudianteCarrera findById(Integer id) {
        return em.find(EstudianteCarrera.class, id);
    }

    @Override
    public List<EstudianteCarrera> findAll() {
        String jpql = "SELECT ec FROM EstudianteCarrera ec";
        TypedQuery<EstudianteCarrera> query = em.createQuery(jpql, EstudianteCarrera.class);
        return query.getResultList();
    }
}
