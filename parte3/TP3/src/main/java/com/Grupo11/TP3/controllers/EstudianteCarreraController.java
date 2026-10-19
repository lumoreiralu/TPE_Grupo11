package com.Grupo11.TP3.controllers;

import com.Grupo11.TP3.models.EstudianteCarrera;
import com.Grupo11.TP3.services.EstudianteCarreraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/estudiantescarreras")
@Tag(name = "Estudiantes y Carreras (Matriculaciones)", description = "Endpoints para la gestión de inscripciones, carreras con alumnos y filtros por ciudad")
public class EstudianteCarreraController {
    @Autowired
    private EstudianteCarreraService estudianteCarreraServicio;

    @Operation(summary = "Matricular un estudiante en una carrera", description = "Registra una nueva inscripción/asociación entre un estudiante y una carrera (Inciso 2.b).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Matriculación realizada con éxito",
                    content = @Content(schema = @Schema(implementation = EstudianteCarrera.class))),
            @ApiResponse(responseCode = "400", description = "Error, no se pudo ingresar, revise los campos")
    })
    @PostMapping("")
    public ResponseEntity<?> save(@RequestBody EstudianteCarrera entity) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(estudianteCarreraServicio.save(entity));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo ingresar, revise los campos e intente nuevamente.\"}");
        }
    }

    @Operation(summary = "Obtener carreras con estudiantes inscriptos", description = "Retorna las carreras que poseen estudiantes inscriptos, ordenadas según criterio institucional (Inciso 2.f).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida con éxito"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/con-estudiantes")
    public ResponseEntity<?> findCarrerasConEstudiantesInscriptosOrdenado() {
        try {
            return ResponseEntity.ok(estudianteCarreraServicio.findCarrerasConEstudiantesInscriptosOrdenado());
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "Estudiantes por carrera y ciudad", description = "Retorna los estudiantes inscriptos en una determinada carrera filtrados por su ciudad de residencia (Inciso 2.g).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiantes obtenidos con éxito"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/id/{id}?ciudad={ciudad}")
    public ResponseEntity<?> findEstudiantesPorCarreraYCiudad(@Parameter(description = "ID de la carrera", example = "1")@PathVariable Long id, @Parameter(description = "Ciudad de residencia para filtrar", example = "Tandil") @RequestParam String ciudad) {
        try {
            return ResponseEntity.ok(estudianteCarreraServicio.findEstudiantesPorCarreraYCiudad(id, ciudad));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }


    @Operation(summary = "Actualizar una matriculación", description = "Actualiza los datos de una inscripción existente según su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Matriculación actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Error al actualizar, revise los campos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@Parameter(description = "ID de la relación a actualizar", example = "1")@PathVariable Long id, @RequestBody EstudianteCarrera entity) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(estudianteCarreraServicio.update(id, entity));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo actualizar, revise los campos e intente nuevamente.\"}");
        }
    }


    @Operation(summary = "Eliminar una matriculación", description = "Elimina una inscripción del sistema según su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "No se pudo eliminar, verifique el ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "ID de la relación a eliminar", example = "1")@PathVariable Long id) {
        try {
            estudianteCarreraServicio.delete(id);
            return ResponseEntity.status(HttpStatus.OK).body("{\"mensaje\":\"Registro eliminado correctamente.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo eliminar el registro, verifique el ID e intente nuevamente.\"}");
        }
    }
}
