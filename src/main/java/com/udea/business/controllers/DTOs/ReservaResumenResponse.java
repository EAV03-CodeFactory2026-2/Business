package com.udea.business.controllers.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Elemento del listado "Mis reservas" (HU-0019): fecha, hora, servicio, negocio y estado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservaResumenResponse {

    @Schema(description = "Identificador de la reserva (para consultar su detalle)", example = "1")
    private Integer idReserva;

    @Schema(description = "Código de referencia de la reserva", example = "RSV-00000001")
    private String codigoReserva;

    @Schema(description = "Fecha de la reserva (yyyy-MM-dd)", example = "2026-11-09")
    private String fecha;

    @Schema(description = "Hora de inicio (HH:mm)", example = "09:00")
    private String horaInicio;

    @Schema(description = "Hora de fin (HH:mm)", example = "09:30")
    private String horaFin;

    @Schema(description = "Nombre del servicio", example = "Corte clásico")
    private String nombreServicio;

    @Schema(description = "Nombre del negocio", example = "Barbería Centro")
    private String nombreNegocio;

    @Schema(description = "Estado visible: Confirmada, Realizada o Cancelada", example = "Confirmada")
    private String estado;
}
