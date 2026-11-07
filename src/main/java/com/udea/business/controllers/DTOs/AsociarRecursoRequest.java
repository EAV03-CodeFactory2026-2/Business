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
public class AsociarRecursoRequest {

    @Schema(description = "Identificador del recurso a asociar", example = "5")
    private Integer recursoId;

    @Schema(description = "Cantidad de unidades que requiere el servicio para una reserva", example = "2")
    private Short cantidadRequerida;
}
