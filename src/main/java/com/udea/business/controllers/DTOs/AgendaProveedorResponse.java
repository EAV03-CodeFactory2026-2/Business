package com.udea.business.controllers.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agenda diaria del proveedor autenticado (HU-0022).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgendaProveedorResponse {

    @Schema(description = "Fecha consultada (yyyy-MM-dd)", example = "2026-11-09")
    private String fecha;

    @Schema(description = "Total de reservas confirmadas del día", example = "2")
    private Integer totalReservas;

    @Schema(description = "Mensaje informativo cuando no hay reservas", example = "No tienes reservas para este día.")
    private String mensaje;

    @Schema(description = "Reservas confirmadas del día ordenadas por hora de inicio")
    private List<ReservaAgendaResponse> reservas;
}
