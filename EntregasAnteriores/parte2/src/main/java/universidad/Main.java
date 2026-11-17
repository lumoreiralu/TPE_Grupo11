package universidad;

import universidad.dto.CarreraInscriptosDTO;
import universidad.dto.ReporteCarreraDTO;
import universidad.factory.RepositoryFactory;
import universidad.repositories.CarreraRepository;
import universidad.repositories.EstudianteCarreraRepository;
import universidad.repositories.EstudianteRepository;
import universidad.entity.Carrera;
import universidad.entity.Estudiante;
import utils.BorrarDatos;
import utils.CargarDatosIniciales;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.EntityManager;

public class Main {
    public static void main(String[] args) {
        RepositoryFactory factory = RepositoryFactory.getInstance();
        EntityManager em = factory.getEntityManager();

        try{
            BorrarDatos borrarDatos = new BorrarDatos(em);
            borrarDatos.run();

            CargarDatosIniciales cargarDatosIniciales = new CargarDatosIniciales(em);
            cargarDatosIniciales.run();


            EstudianteRepository estudianteRepo = factory.getEstudianteRepository(); //Es la instancia u objeto concreto que te devuelve la fábrica (`factory`). Es la herramienta que esta guardada en memoria para comunicarse con la base de datos.
            EstudianteCarreraRepository estudianteCarreraRepo = factory.getEstudianteCarreraRepository();
            CarreraRepository carreraRepo = factory.getCarreraRepository();




            System.out.println("Prueba dar de alta un estudiante. Punto 2. a. ");
            Estudiante nuevo = new Estudiante(1234, "Ana", "Lopez", "F", "Azul", 109845, LocalDate.of(1996,04,17)); //creo un estudiante
            em.getTransaction().begin();
            estudianteRepo.save(nuevo);
            em.getTransaction().commit();


            System.out.println("Prueba de matricular un estudiante a una carrera. Punto 2. b. ");
            // 2\. Buscar el estudiante y la carrera existentes en la BD
            Estudiante estudiante11 = estudianteRepo.findByDni(39279226);
            Carrera carrera11 = carreraRepo.findById(1);

            if (estudiante11 != null && carrera11 != null) {
                em.getTransaction().begin();
                // Llamamos al método matricular de nuestro repositorio
                estudianteCarreraRepo.matricular( estudiante11, carrera11, null);
                em.getTransaction().commit();
                System.out.println("¡Estudiante matriculado con éxito!");
            }


            System.out.println("Prueba del punto C");
            List<Estudiante> estudiantesOrdenadosPorEdad = estudianteRepo.findAllOrderedByEdadAsc();
            System.out.println("Estudiantes ordenados por edad ascendente:");
            for (Estudiante estudiante : estudiantesOrdenadosPorEdad) {
                System.out.println(estudiante);
            }


            System.out.println("Prueba del punto d");
            Estudiante porLU = estudianteRepo.findByLU(34978);
            System.out.println("Buscado por LU 34978: " + porLU);

            Estudiante lu31733344 = estudianteRepo.findByLU(31733344);
            System.out.println("Buscado por LU 31733344: " + lu31733344);


            System.out.println("Prueba del punto e");
            List<Estudiante> mujeres = estudianteRepo.findByGenero("Female");
            System.out.println("Cantidad de estudiantes genero Female: " + mujeres.size());


            System.out.println("Prueba del punto f");
            List<CarreraInscriptosDTO> carrerasConInscriptos = carreraRepo.findAllConInscriptosOrderedByCantInscriptosDesc();
            System.out.println("Carreras con inscriptos ordenadas por cantidad de inscriptos descendente:");
            for (CarreraInscriptosDTO dto : carrerasConInscriptos) {
                System.out.println(dto);
            }


            System.out.println("Prueba del punto g");
            List<Estudiante> estudiantesPorCarreraYCiudad = estudianteRepo.findByCarreraAndCiudad(7, "Sámi");
            System.out.println("\n- Estudiantes de la carrera (id: 7) filtrados por ciudad ('Sámi')-");
            if (estudiantesPorCarreraYCiudad.isEmpty()) {
                System.out.println("No se encontraron estudiantes para los criterios especificados.");
            } else {
                for (Estudiante e : estudiantesPorCarreraYCiudad) {
                    System.out.println(e);
                }
            }

            System.out.println("Prueba del punto 3");
            List<ReporteCarreraDTO> reporte = carreraRepo.getReporteCarreras();
            System.out.println("Reporte carrera:");
            for (ReporteCarreraDTO dto : reporte) {
                System.out.println(dto);
            }

        }catch (Exception e){
            e.printStackTrace();
        } finally{
            factory.close();
        }

    }

}
