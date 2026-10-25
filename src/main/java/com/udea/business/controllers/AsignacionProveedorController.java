package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.AsignacionProveedoresResponse;
import com.udea.business.controllers.DTOs.AsignarProveedoresRequest;
import com.udea.business.services.IAsignacionProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/servicios/{servicioId}/proveedores")
@RequiredArgsConstructor
@Tag(name = "Asignación de proveedores", description = "Vinculación de servicios del catálogo con proveedores")
public class AsignacionProveedorController {

    private final IAsignacionProveedorService asignacionProveedorService;

    @Operation(
            summary = "Asignar proveedores a un servicio",
            description = "Vincula el servicio con uno o varios proveedores en una sola operación: se guardan todos "
                    + "o ninguno. El servicio queda en estado Asignado. Requiere rol Propietario y ser dueño del negocio."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proveedores vinculados."),
            @ApiResponse(responseCode = "400", description = "Sin selección, proveedores repetidos o usuarios que no son proveedores."),
            @ApiResponse(responseCode = "403", description = "El rol activo no es Propietario o el servicio no es de sus negocios."),
            @ApiResponse(responseCode = "404", description = "El servicio no existe."),
            @ApiResponse(responseCode = "409", description = "Algún proveedor ya está vinculado al servicio.")
    })
    @PostMapping
    public ResponseEntity<AsignacionProveedoresResponse> asignar(@PathVariable("servicioId") Integer servicioId,
                                                                 @RequestBody(required = false) AsignarProveedoresRequest request) {
        AsignacionProveedoresResponse respuesta = asignacionProveedorService.asignar(
                servicioId, Optional.ofNullable(request).map(AsignarProveedoresRequest::getProveedorIds).orElse(null));
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(
            summary = "Consultar proveedores asignados a un servicio",
            description = "Recupera la lista de proveedores asociados al servicio indicado. "
                    + "Requiere rol Propietario y ser dueño del negocio del servicio."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proveedores asignados consultados exitosamente."),
            @ApiResponse(responseCode = "403", description = "El rol activo no es Propietario o el servicio no es de sus negocios."),
            @ApiResponse(responseCode = "404", description = "El servicio no existe.")
    })
    @GetMapping
    public ResponseEntity<java.util.List<com.udea.business.controllers.DTOs.ProveedorAsignadoResponse>> consultarProveedores(
            @PathVariable("servicioId") Integer servicioId) {
        return ResponseEntity.ok(asignacionProveedorService.consultarProveedoresAsignados(servicioId));
    }
}
