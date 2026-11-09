package com.udea.business.controllers.DTOs;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearReservaRequest {

    @Schema(description = "Identificador de la franja horaria definida (opcional si se especifican servicio, proveedor, fecha y horaInicio)", example = "10")
    private Integer franjaId;

    @Schema(description = "Identificador del servicio a reservar", example = "1")
    private Integer servicioId;

    @Schema(description = "Identificador del proveedor seleccionado", example = "12")
    private Integer proveedorId;

    @Schema(description = "Fecha de la cita en formato yyyy-MM-dd", example = "2026-11-09")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    @Schema(description = "Hora de inicio de la cita en formato HH:mm", example = "09:00")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime horaInicio;

    @Schema(description = "Notas adicionales para el proveedor (máximo 250 caracteres)", example = "Corte bajo y desvanecido en los laterales")
    private String notas;
}
