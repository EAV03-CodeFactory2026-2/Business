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
public class RecursoRequeridoResponse {

    @Schema(description = "Identificador del recurso", example = "5")
    private Integer idRecurso;

    @Schema(description = "Nombre descriptivo del recurso", example = "Camilla")
    private String nombreRecurso;

    @Schema(description = "Cantidad de unidades requeridas por el servicio", example = "2")
    private Short cantidadRequerida;
}
