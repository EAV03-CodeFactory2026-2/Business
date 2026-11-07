package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.AsociacionRecursoResponse;
import com.udea.business.controllers.DTOs.AsociarRecursoRequest;
import com.udea.business.controllers.DTOs.RecursoRequeridoResponse;
import com.udea.business.services.IServicioRecursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/servicios/{servicioId}/recursos")
@RequiredArgsConstructor
@Tag(name = "Recursos de servicio", description = "Asociación y gestión de recursos requeridos para los servicios del negocio")
public class ServicioRecursoController {

    private final IServicioRecursoService servicioRecursoService;

    @Operation(
            summary = "Asociar un recurso a un servicio",
            description = "Asocia un recurso físico indicando la cantidad de unidades requeridas por reserva. "
                    + "Solo el propietario del negocio puede asociar recursos a sus servicios."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recurso asociado al servicio."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, cantidad mayor a las disponibles o recurso de otro negocio."),
            @ApiResponse(responseCode = "403", description = "No tienes permiso para modificar este servicio o no eres propietario."),
            @ApiResponse(responseCode = "404", description = "Servicio o recurso no encontrado."),
            @ApiResponse(responseCode = "409", description = "Este recurso ya está asociado al servicio.")
    })
    @PostMapping
    public ResponseEntity<AsociacionRecursoResponse> asociarRecurso(
            @PathVariable("servicioId") Integer servicioId,
            @RequestBody(required = false) AsociarRecursoRequest request) {
        AsociacionRecursoResponse respuesta = servicioRecursoService.asociarRecurso(servicioId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(
            summary = "Consultar recursos requeridos por un servicio",
            description = "Obtiene la lista de recursos y cantidades requeridas por el servicio especificado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recursos consultados exitosamente."),
            @ApiResponse(responseCode = "403", description = "No tienes permiso para consultar este servicio."),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado.")
    })
    @GetMapping
    public ResponseEntity<List<RecursoRequeridoResponse>> consultarRecursos(
            @PathVariable("servicioId") Integer servicioId) {
        List<RecursoRequeridoResponse> recursos = servicioRecursoService.consultarRecursosRequeridos(servicioId);
        return ResponseEntity.ok(recursos);
    }

    @Operation(
            summary = "Quitar un recurso de un servicio",
            description = "Elimina la asociación entre un recurso y un servicio. Las reservas existentes conservan lo que consumieron."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Recurso quitado del servicio exitosamente."),
            @ApiResponse(responseCode = "403", description = "No tienes permiso para modificar este servicio."),
            @ApiResponse(responseCode = "404", description = "Servicio o recurso no encontrado.")
    })
    @DeleteMapping("/{recursoId}")
    public ResponseEntity<Void> quitarRecurso(
            @PathVariable("servicioId") Integer servicioId,
            @PathVariable("recursoId") Integer recursoId) {
        servicioRecursoService.quitarRecurso(servicioId, recursoId);
        return ResponseEntity.noContent().build();
    }
}
