package universidad.repositories;

import universidad.entity.Estudiante;

import javax.persistence.EntityManager;
import java.util.List;
import javax.persistence.TypedQuery;

public class EstudianteRepositoryImpl implements EstudianteRepository {
    private final EntityManager em;

    public EstudianteRepositoryImpl(EntityManager em) {
        this.em = em;
    }


    @Override
    public Estudiante save(Estudiante estudiante) {
        if(em.find(Estudiante.class, estudiante.getDni()) == null){
            em.persist(estudiante);
        }else{
            estudiante = em.merge(estudiante);
        }
        return estudiante;
    }

    @Override
    public Estudiante findByDni(Integer dni) {
        return em.find(Estudiante.class, dni);
    }

    @Override 
    public List<Estudiante> findAllOrderedByEdadAsc() {
        String jpql = "SELECT e FROM Estudiante e ORDER BY e.edad ASC";
        TypedQuery<Estudiante> query = em.createQuery(jpql, Estudiante.class);
        return query.getResultList();
    }

    @Override
    public Estudiante findByLU(int numLU) {
        String jpql = "SELECT e FROM Estudiante e WHERE e.libretaUnica = :lu";
        TypedQuery<Estudiante> query = em.createQuery(jpql, Estudiante.class);
        query.setParameter("lu", numLU);
        List<Estudiante> resultado = query.getResultList();
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    @Override
    public List<Estudiante> findByGenero(String genero) {
        String jpql = "SELECT e FROM Estudiante e WHERE e.genero = :genero";
        TypedQuery<Estudiante> query = em.createQuery(jpql, Estudiante.class);
        query.setParameter("genero", genero);
        return query.getResultList();
    }

    @Override
    public List<Estudiante> findByCarreraAndCiudad(Integer idCarrera, String ciudad) {
        String jpql = "SELECT ec.estudiante FROM EstudianteCarrera ec " +
                "WHERE ec.carrera.id = :idCarrera AND LOWER(ec.estudiante.ciudadResidencia) = LOWER(:ciudad)";
        TypedQuery<Estudiante> query = em.createQuery(jpql, Estudiante.class);
        query.setParameter("idCarrera", idCarrera);
        query.setParameter("ciudad", ciudad);
        return query.getResultList();
    }
}
