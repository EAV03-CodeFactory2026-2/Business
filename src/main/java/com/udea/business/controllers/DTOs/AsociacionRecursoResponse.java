package com.udea.business.controllers.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsociacionRecursoResponse {

    @Schema(description = "Mensaje informativo del resultado de la operación", example = "Recurso asociado al servicio.")
    private String mensaje;

    @Schema(description = "Identificador del servicio", example = "10")
    private Integer idServicio;

    @Schema(description = "Lista completa actualizada de los recursos requeridos por el servicio")
    private List<RecursoRequeridoResponse> recursosRequeridos;
}
