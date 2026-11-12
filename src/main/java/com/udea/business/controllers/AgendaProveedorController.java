package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.AgendaProveedorResponse;
import com.udea.business.services.IAgendaProveedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * HU-0022 · Consultar mi agenda diaria de proveedor.
 */
@RestController
@RequestMapping("/api/v1/reservas/mi-agenda")
@RequiredArgsConstructor
@Tag(name = "Agenda del proveedor", description = "Agenda diaria del proveedor autenticado")
public class AgendaProveedorController {

    private final IAgendaProveedorService agendaProveedorService;

    @Operation(
            summary = "Consultar mi agenda diaria",
            description = "Lista las reservas confirmadas del proveedor autenticado para un día, en todos sus negocios, "
                    + "ordenadas por hora de inicio, con el contacto y las notas del cliente. "
                    + "Si no se indica fecha se usa el día actual.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agenda consultada."),
            @ApiResponse(responseCode = "400", description = "La fecha no tiene un formato válido."),
            @ApiResponse(responseCode = "403", description = "Solo los proveedores pueden consultar su agenda.")
    })
    @GetMapping
    public ResponseEntity<AgendaProveedorResponse> consultarMiAgenda(
            @Parameter(description = "Día a consultar (YYYY-MM-DD). Por defecto, hoy.", example = "2026-11-09")
            @RequestParam(value = "fecha", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(agendaProveedorService.consultarMiAgenda(fecha));
    }
}
