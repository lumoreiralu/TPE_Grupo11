package universidad.factory;

import universidad.repositories.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class RepositoryFactory {
    private static RepositoryFactory instance;
    private EntityManagerFactory emf;
    private EntityManager em;

    private RepositoryFactory(){
        this.emf= Persistence.createEntityManagerFactory("TPE2");
        this.em= this.emf.createEntityManager();
    }

    public static synchronized RepositoryFactory getInstance(){
        if(instance==null){
            instance = new RepositoryFactory();
        }

        return instance;
    }

    public EstudianteRepository getEstudianteRepository(){
        return new EstudianteRepositoryImpl(this.em);
    }

    public CarreraRepository getCarreraRepository(){
        return new CarreraRepositoryImpl(this.em);
    }

    public EstudianteCarreraRepository getEstudianteCarreraRepository(){
        return new EstudianteCarreraRepositoryImpl(this.em);
    }

    public EntityManager getEntityManager(){
        return this.em;
    }

    public void close(){
        if(this.em!=null && this.em.isOpen()){
            this.em.close();
        }
        if(this.emf!=null && this.emf.isOpen()){
            this.emf.close();
        }
    }


}
