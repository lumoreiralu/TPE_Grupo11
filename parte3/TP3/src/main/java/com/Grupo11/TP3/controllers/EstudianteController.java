package com.Grupo11.TP3.controllers;

import com.Grupo11.TP3.models.Estudiante;
import com.Grupo11.TP3.services.EstudianteService;
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

import java.util.List;

@RestController
@RequestMapping("/api/estudiantes")
@Tag(name = "Estudiantes", description = "Endpoints para la gestión, consultas y filtros de estudiantes")
public class EstudianteController
{
    @Autowired
    private EstudianteService estudianteServicio;

    @Operation(summary = "Obtener estudiantes por género", description = "Frena y retorna la lista de estudiantes filtrados por su género (Inciso 2.e).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiantes obtenidos con éxito"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @GetMapping("/genero/{genero}")
    public ResponseEntity<?> getEstudiantesPorGenero(@Parameter(description = "Género por el cual filtrar", example = "Masculino") @PathVariable String genero) {
        try {
            return ResponseEntity.ok(estudianteServicio.getEstudiantesPorGenero(genero));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @Operation(summary = "Registrar un nuevo estudiante", description = "Crea y almacena un nuevo estudiante en el sistema (Inciso 2.a).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiante registrado con éxito",
                    content = @Content(schema = @Schema(implementation = Estudiante.class))),
            @ApiResponse(responseCode = "400", description = "No se pudo cargar al estudiante (error en los datos)")
    })
    @PostMapping("")
    public ResponseEntity<?> save(@RequestBody Estudiante estudiante){
        try{
            return ResponseEntity.status(HttpStatus.OK).body(estudianteServicio.save(estudiante));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"No se pudo cargar al estudiante.\"}");
        }
    }
    @Operation(summary = "Buscar estudiante por Libreta Universitaria (LU)", description = "Retorna un estudiante específico según su número de LU (Inciso 2.d).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiante encontrado"),
            @ApiResponse(responseCode = "404", description = "No se encontró el estudiante")
    })
    @GetMapping("/lu/{lu}")
    public ResponseEntity<?> getEstudianteByLU(@Parameter(description = "Número de Libreta Universitaria", example = "12345")@PathVariable int lu){
        try{
            return ResponseEntity.status(HttpStatus.OK).body(estudianteServicio.buscarEstudiantePorLU(lu));
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":\"No se encontro el estudiante.\"}");
        }
    }

    @Operation(summary = "Obtener estudiantes ordenados por apellido", description = "Retorna la lista de estudiantes ordenada alfabéticamente por apellido (Inciso 2.c).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida con éxito"),
            @ApiResponse(responseCode = "404", description = "Error al obtener la lista")
    })
    @GetMapping("/ordenados-por-apellido")
    public ResponseEntity<?> getEstudiantesOrdenados() {
        try {
            List<Estudiante> estudiantes = estudianteServicio.getEstudiantesOrdenadosPorApellido();
            return ResponseEntity.status(HttpStatus.OK).body(estudiantes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Error al obtener la lista de estudiantes.\"}");
        }
    }

    @Operation(summary = "Actualizar un estudiante", description = "Actualiza los datos de un estudiante existente según su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estudiante actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Error al actualizar, revise los campos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@Parameter(description = "ID único del estudiante a actualizar", example = "1")@PathVariable Long id, @RequestBody Estudiante entity) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(estudianteServicio.update(id, entity));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo actualizar, revise los campos e intente nuevamente.\"}");
        }
    }

    @Operation(summary = "Eliminar un estudiante", description = "Elimina un registro de estudiante del sistema según su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "No se pudo eliminar, verifique el ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "ID único del estudiante a eliminar", example = "1")@PathVariable Long id) {
        try {
            estudianteServicio.delete(id);
            return ResponseEntity.status(HttpStatus.OK).body("{\"mensaje\":\"Registro eliminado correctamente.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo eliminar el registro, verifique el ID e intente nuevamente.\"}");
        }
    }

    @Operation(summary = "Obtener todos los estudiantes", description = "Retorna una lista completa con todos los estudiantes registrados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista obtenida con éxito"),
            @ApiResponse(responseCode = "404", description = "Error al obtener la lista")
    })
    @GetMapping("")
    public ResponseEntity<?> getAllEstudiantes() {
        try {
            List<Estudiante> estudiantes = estudianteServicio.findAll();
            return ResponseEntity.status(HttpStatus.OK).body(estudiantes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Error al obtener la lista de estudiantes.\"}");
        }
    }
}
