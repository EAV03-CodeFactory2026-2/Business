package com.udea.business.controllers.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FranjaDisponibleResponse {

    @Schema(description = "Identificador de la franja horaria", example = "10")
    private Integer idFranja;

    @Schema(description = "Identificador del servicio", example = "1")
    private Integer servicioId;

    @Schema(description = "Nombre del servicio", example = "Corte clásico")
    private String nombreServicio;

    @Schema(description = "Identificador del proveedor asignado", example = "12")
    private Integer proveedorId;

    @Schema(description = "Nombre completo del proveedor", example = "Carlos Ruiz")
    private String nombreProveedor;

    @Schema(description = "Fecha consultada en formato yyyy-MM-dd", example = "2026-11-09")
    private String fecha;

    @Schema(description = "Día de la semana de la franja", example = "LUNES")
    private String diaSemana;

    @Schema(description = "Hora de inicio en formato HH:mm", example = "09:00")
    private String horaInicio;

    @Schema(description = "Hora de fin estimada en formato HH:mm", example = "09:30")
    private String horaFin;

    @Schema(description = "Duración del servicio en minutos", example = "30")
    private Integer duracionMinutos;

    @Schema(description = "Precio vigente del servicio", example = "25000.00")
    private BigDecimal precio;

    @Schema(description = "Código ISO de la moneda", example = "COP")
    private String moneda;
}
