package com.udea.business.controllers.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reserva tal como la ve el proveedor en su agenda diaria (HU-0022).
 * Incluye el contacto del cliente, que solo debe usarse para gestionar la reserva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservaAgendaResponse {

    @Schema(description = "Identificador de la reserva", example = "1")
    private Integer idReserva;

    @Schema(description = "Código de referencia de la reserva", example = "RSV-00000001")
    private String codigoReserva;

    @Schema(description = "Hora de inicio (HH:mm)", example = "09:00")
    private String horaInicio;

    @Schema(description = "Hora de fin (HH:mm)", example = "09:30")
    private String horaFin;

    @Schema(description = "Nombre del servicio", example = "Corte clásico")
    private String nombreServicio;

    @Schema(description = "Nombre del negocio", example = "Barbería Centro")
    private String nombreNegocio;

    @Schema(description = "Nombre del cliente", example = "Laura Pérez")
    private String nombreCliente;

    @Schema(description = "Teléfono del cliente", example = "+573001234567")
    private String telefonoCliente;

    @Schema(description = "Notas que dejó el cliente al reservar", example = "Prefiero corte con tijera")
    private String notas;
}
