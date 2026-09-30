package utils;


import javax.persistence.EntityManager;

public class BorrarDatos {
    private final EntityManager em;

    public BorrarDatos(EntityManager em) {
        this.em = em;
    }

    public void run() {
        em.getTransaction().begin();
        try{
            em.createQuery("DELETE FROM EstudianteCarrera").executeUpdate();

            em.createQuery("DELETE FROM Carrera").executeUpdate();
            em.createQuery("DELETE FROM Estudiante").executeUpdate();

            em.getTransaction().commit();
            em.clear();
            System.out.println("Borrado completo finalizado con exito");
        } catch (Exception e){
            if(em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            throw new RuntimeException("Error al borrar masivamente", e);
        }
    }
}
