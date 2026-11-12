package com.udea.business.controllers.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Reservas del cliente autenticado agrupadas en "Próximas" e "Historial" (HU-0019).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MisReservasResponse {

    @Schema(description = "Mensaje informativo cuando el cliente no tiene reservas", example = "Aún no tienes reservas.")
    private String mensaje;

    @Schema(description = "Reservas confirmadas que aún no inician, de la más cercana a la más lejana")
    private List<ReservaResumenResponse> proximas;

    @Schema(description = "Reservas realizadas y canceladas, de la más reciente a la más antigua")
    private List<ReservaResumenResponse> historial;
}
