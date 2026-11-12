package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.MisReservasResponse;
import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.services.IMisReservasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HU-0019 · Consultar mis reservas y su detalle.
 */
@RestController
@RequestMapping("/api/v1/reservas/mis-reservas")
@RequiredArgsConstructor
@Tag(name = "Mis reservas", description = "Consulta de reservas propias del cliente autenticado")
public class MisReservasController {

    private final IMisReservasService misReservasService;

    @Operation(
            summary = "Consultar mis reservas",
            description = "Devuelve las reservas del cliente autenticado agrupadas en 'proximas' (confirmadas que aún no "
                    + "inician, de la más cercana a la más lejana) e 'historial' (realizadas y canceladas, de la más "
                    + "reciente a la más antigua).",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservas del cliente consultadas."),
            @ApiResponse(responseCode = "403", description = "Solo los clientes pueden consultar sus reservas.")
    })
    @GetMapping
    public ResponseEntity<MisReservasResponse> consultarMisReservas() {
        return ResponseEntity.ok(misReservasService.consultarMisReservas());
    }

    @Operation(
            summary = "Consultar el detalle de una reserva propia",
            description = "Devuelve código, estado, negocio, servicio, proveedor, fecha, horas, duración, fecha de creación "
                    + "y, si fue cancelada, la fecha y el motivo de la cancelación.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la reserva."),
            @ApiResponse(responseCode = "403", description = "Solo los clientes pueden consultar sus reservas."),
            @ApiResponse(responseCode = "404", description = "La reserva no existe o no pertenece al cliente.")
    })
    @GetMapping("/{idReserva}")
    public ResponseEntity<ReservaResponse> consultarDetalle(@PathVariable("idReserva") Integer idReserva) {
        return ResponseEntity.ok(misReservasService.consultarDetalle(idReserva));
    }
}
