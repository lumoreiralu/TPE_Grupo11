package com.Grupo11.TP3.controllers;

import com.Grupo11.TP3.models.Carrera;
import com.Grupo11.TP3.services.CarreraService;
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
@RequestMapping("/api/carreras")
@Tag(name = "Carreras", description = "Endpoints para la gestión y reportes de carreras")
public class CarreraController {
    @Autowired
    private CarreraService carreraServicio;

    @Operation(summary = "Registrar una nueva carrera", description = "Crea y almacena una nueva carrera en la base de datos.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carrera creada con éxito",
                    content = @Content(schema = @Schema(implementation = Carrera.class))),
            @ApiResponse(responseCode = "400", description = "No se pudo cargar la carrera (error en los datos enviados)")
    })
    @PostMapping("")
    public ResponseEntity<?> save(@RequestBody Carrera carrera){
        try{
            return ResponseEntity.status(HttpStatus.OK).body(carreraServicio.save(carrera));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"No se pudo cargar la carrera.\"}");
        }
    }


    @Operation(summary = "Obtener todas las carreras", description = "Retorna una lista con todas las carreras registradas en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de carreras obtenida con éxito"),
            @ApiResponse(responseCode = "404", description = "Error al obtener la lista")
    })
    @GetMapping("")
    public ResponseEntity<?> getAllCarreras() {
        try {
            List<Carrera> carreras = carreraServicio.findAll();
            return ResponseEntity.status(HttpStatus.OK).body(carreras);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Error al obtener la lista de carreras.\"}");
        }
    }


    @Operation(summary = "Reporte de carreras", description = "Retorna el reporte de inscriptos y egresados por año por cada carrera (Inciso 2.h).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reporte generado con éxito"),
            @ApiResponse(responseCode = "500", description = "Error interno al generar el reporte")
    })
    // GET /carreras/reporte
    @GetMapping("/reporte")
    public ResponseEntity<?> getReporteCarreras() {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(carreraServicio.getReporteCarreras());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Error al generar el reporte de carreras.\"}");
        }
    }


    @Operation(summary = "Actualizar una carrera", description = "Actualiza los datos de una carrera existente según su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carrera actualizada con éxito"),
            @ApiResponse(responseCode = "400", description = "Error al actualizar, revise los campos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@Parameter(description = "ID único de la carrera a actualizar", example = "1")@PathVariable Long id, @RequestBody Carrera entity) {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(carreraServicio.update(id, entity));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo actualizar, revise los campos e intente nuevamente.\"}");
        }
    }

    @Operation(summary = "Eliminar una carrera", description = "Elimina un registro de carrera del sistema según su ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Registro eliminado correctamente"),
            @ApiResponse(responseCode = "400", description = "No se pudo eliminar, verifique el ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@Parameter(description = "ID único de la carrera a eliminar", example = "1")@PathVariable Long id) {
        try {
            carreraServicio.delete(id);
            return ResponseEntity.status(HttpStatus.OK).body("{\"mensaje\":\"Registro eliminado correctamente.\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Error. No se pudo eliminar el registro, verifique el ID e intente nuevamente.\"}");
        }
    }

}