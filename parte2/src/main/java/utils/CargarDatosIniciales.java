package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import universidad.entity.Estudiante;
import universidad.entity.Carrera;
import universidad.entity.EstudianteCarrera;

import javax.persistence.EntityManager;
public class CargarDatosIniciales {

    private final EntityManager em;

    public CargarDatosIniciales(EntityManager em) {
        this.em = em;
    }


    public void run() {
        em.getTransaction().begin();
        try {
            cargarCarreras("/data/carreras.csv");
            cargarEstudiantes("/data/estudiantes.csv");
            cargarEstudiantesCarreras("/data/estudianteCarrera.csv");

            em.getTransaction().commit();
            System.out.println("Carga de datos finalizada con exito...");

        } catch (Exception e) {
            if(em.getTransaction().isActive()){
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        }
    }

    private void cargarCarreras(String resourcePath) {
        try (InputStream is = mustGetResource(resourcePath);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
                for (CSVRecord row : parser) {
                    Integer idCarrera = Integer.parseInt(row.get("id_carrera"));
                    String nombre = row.get("carrera");
                    int duracion = Integer.parseInt(row.get("duracion"));
                    Carrera c = new Carrera(idCarrera, nombre, duracion);

                    em.persist(c);
                }
        } catch (Exception e) {
            throw new RuntimeException("Fallo al cargar el archivo. Deteniendo proceso.", e);
        }
    }

    private void cargarEstudiantes(String resourcePath) {
        try (InputStream is = mustGetResource(resourcePath);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
                for (CSVRecord row : parser) {
                    // Adaptar los nombres de columnas a tu CSV
                    int dni = Integer.parseInt(row.get("DNI"));
                    String nombre = row.get("nombre");
                    String apellido = row.get("apellido");
                    int edad = Integer.parseInt(row.get("edad"));
                    String genero = row.get("genero");
                    String ciudad = row.get("ciudad");
                    int lu = Integer.parseInt(row.get("LU"));
                    Estudiante e = new Estudiante(dni, nombre, apellido, edad, genero, ciudad, lu);
                    em.persist(e);
                }
        } catch (Exception e){
            throw new RuntimeException("Fallo al cargar el archivo. Deteniendo proceso.", e);
        }
    }

    private void cargarEstudiantesCarreras(String resourcePath) {
        try (InputStream is = mustGetResource(resourcePath);
             Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
                for (CSVRecord row : parser) {
                    // Obtener los identificadores y atributos del CSV
                    int idEstudiante = Integer.parseInt(row.get("id_estudiante"));
                    int idCarrera = Integer.parseInt(row.get("id_carrera"));
                    int inscripcion = Integer.parseInt(row.get("inscripcion"));
                    Integer graduacion = Integer.parseInt(row.get("graduacion"));

                    // Buscar las entidades persistidas en el contexto de JPA
                    Estudiante e = em.find(Estudiante.class, idEstudiante);
                    Carrera c = em.find(Carrera.class, idCarrera);
                    if (e != null && c != null) {
                        EstudianteCarrera ec = new EstudianteCarrera(c, e, inscripcion, graduacion);
                        em.merge(ec);
                    }
                }
        } catch (Exception e) {
            throw new RuntimeException("Fallo al cargar el archivo. Deteniendo proceso.", e);
        }
    }


    // --- util ---
    private InputStream mustGetResource(String path) {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) throw new IllegalArgumentException("Recurso no encontrado: " + path);
        return is;
    }
}