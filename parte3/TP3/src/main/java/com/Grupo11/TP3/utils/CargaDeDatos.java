package com.Grupo11.TP3.utils;

import com.Grupo11.TP3.models.Carrera;
import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.models.EstudianteCarrera;
import com.Grupo11.TP3.repository.CarreraRepository;
import com.Grupo11.TP3.repository.EstudianteCarreraRepository;
import com.Grupo11.TP3.repository.EstudianteRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Component
public class CargaDeDatos {

    private final EstudianteRepository estudianteRepository;
    private final CarreraRepository carreraRepository;
    private final EstudianteCarreraRepository estudianteCarreraRepository;

    @Autowired
    public CargaDeDatos(EstudianteRepository estudianteRepository, CarreraRepository carreraRepository, EstudianteCarreraRepository estudianteCarreraRepository) {
        this.estudianteRepository = estudianteRepository;
        this.carreraRepository = carreraRepository;
        this.estudianteCarreraRepository = estudianteCarreraRepository;
    }

    public void cargarDatosDesdeCSV() throws IOException {
        // Importante: Primero cargamos las entidades independientes
        cargarCarreras();
        cargarEstudiantes();
        // Por último la tabla intermedia que depende de los dos anteriores
        cargarMatriculaciones();
    }

    private void cargarCarreras() throws IOException {
        File archivoCSV = ResourceUtils.getFile("src/main/resources/csv/carreras.csv");
        try (FileReader reader = new FileReader(archivoCSV);
             CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            for (CSVRecord csvRecord : csvParser) {
                Carrera carrera = new Carrera();
                carrera.setId(Long.parseLong(csvRecord.get("id_carrera")));
                carrera.setNombre(csvRecord.get("carrera"));
                // Si tu entidad tiene duración, podés descomentar la siguiente línea:
                // carrera.setDuracion(Integer.parseInt(csvRecord.get("duracion")));

                carreraRepository.save(carrera);
            }
        }
    }

    private void cargarEstudiantes() throws IOException {
        File archivoCSV = ResourceUtils.getFile("src/main/resources/csv/estudiantes.csv");
        try (FileReader reader = new FileReader(archivoCSV);
             CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            for (CSVRecord csvRecord : csvParser) {
                Estudiante e = new Estudiante();
                // Ojo: Asegurate de que el campo clave en tu entidad Estudiante sea el DNI o el ID correspondiente
                e.setDni(Long.parseLong(csvRecord.get("DNI")));
                e.setNombre(csvRecord.get("nombre"));
                e.setApellido(csvRecord.get("apellido"));
                e.setEdad(Integer.parseInt(csvRecord.get("edad")));
                e.setGenero(csvRecord.get("genero"));
                e.setCiudadResidencia(csvRecord.get("ciudad"));
                e.setNumLibretaUni(Integer.parseInt(csvRecord.get("LU")));

                estudianteRepository.save(e);
            }
        }
    }

    private void cargarMatriculaciones() throws IOException {
        File archivoCSV = ResourceUtils.getFile("src/main/resources/csv/estudianteCarrera.csv");
        try (FileReader reader = new FileReader(archivoCSV);
             CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            for (CSVRecord csvRecord : csvParser) {
                // En tu CSV de matriculaciones, id_estudiante es en realidad el DNI
                int dniEstudiante = Long.parseLong(csvRecord.get("id_estudiante"));
                int idCarrera = Integer.parseInt(csvRecord.get("id_carrera"));
                int inscripcion = Integer.parseInt(csvRecord.get("inscripcion"));
                int graduacion = Integer.parseInt(csvRecord.get("graduacion"));

                // Si en tu clase EstudianteCarrera no tenés el campo antigüedad, podés omitirlo.
                // Acá asumimos que buscás por el DNI/ID con el que guardaste al estudiante:
                Estudiante e = estudianteRepository.findById(dniEstudiante).orElse(null);
                Carrera c = carreraRepository.findById(idCarrera).orElse(null);

                if (e != null && c != null) {
                    EstudianteCarrera ec = new EstudianteCarrera(c, e, inscripcion, graduacion);
                    estudianteCarreraRepository.save(ec);
                }
            }
        }
    }
}