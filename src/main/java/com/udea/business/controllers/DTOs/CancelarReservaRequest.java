package com.udea.business.controllers.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelarReservaRequest {

    @Schema(description = "Motivo opcional por el cual el cliente cancela la reserva", example = "Imprevisto personal de fuerza mayor", maxLength = 250)
    private String motivo;

    @Schema(description = "Confirmación explícita de la acción de cancelación", example = "true", defaultValue = "true")
    private Boolean confirmacion;
}
