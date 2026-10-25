package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.ServicioAsignadoResponse;
import com.udea.business.services.IAsignacionProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proveedores/mis-servicios")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Operaciones de consulta y gestión para proveedores")
public class MisServiciosController {

    private final IAsignacionProveedorService asignacionProveedorService;

    @Operation(
            summary = "Consultar servicios asignados al proveedor autenticado",
            description = "Recupera la lista de servicios asignados al proveedor identificado mediante su token JWT. "
                    + "Requiere rol Proveedor."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servicios consultados exitosamente."),
            @ApiResponse(responseCode = "403", description = "El rol activo no es Proveedor o falta autorización.")
    })
    @GetMapping
    public ResponseEntity<List<ServicioAsignadoResponse>> consultarMisServicios() {
        return ResponseEntity.ok(asignacionProveedorService.consultarMisServicios());
    }
}
