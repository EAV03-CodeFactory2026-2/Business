package com.udea.business.controllers.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReservaResponse {

    @Schema(description = "Mensaje informativo de la reserva", example = "Reserva confirmada exitosamente.")
    private String mensaje;

    @Schema(description = "Identificador único de la reserva", example = "1")
    private Integer idReserva;

    @Schema(description = "Código único de referencia de la reserva", example = "RSV-00000001")
    private String codigoReserva;

    @Schema(description = "Estado actual de la reserva", example = "Confirmada")
    private String estado;

    @Schema(description = "Identificador del servicio reservado", example = "1")
    private Integer servicioId;

    @Schema(description = "Nombre del servicio al momento de reservar", example = "Corte clásico")
    private String nombreServicio;

    @Schema(description = "Identificador del negocio", example = "10")
    private Integer negocioId;

    @Schema(description = "Nombre del negocio donde se prestará el servicio", example = "Barbería Centro")
    private String nombreNegocio;

    @Schema(description = "Identificador del proveedor asignado", example = "12")
    private Integer proveedorId;

    @Schema(description = "Nombre del proveedor asignado", example = "Carlos Ruiz")
    private String nombreProveedor;

    @Schema(description = "Identificador del cliente", example = "20")
    private Integer clienteId;

    @Schema(description = "Nombre del cliente que realizó la reserva", example = "Laura Pérez")
    private String nombreCliente;

    @Schema(description = "Fecha de la reserva en formato yyyy-MM-dd", example = "2026-11-09")
    private String fecha;

    @Schema(description = "Hora de inicio en formato HH:mm", example = "09:00")
    private String horaInicio;

    @Schema(description = "Hora de fin calculada en formato HH:mm", example = "09:30")
    private String horaFin;

    @Schema(description = "Duración del servicio en minutos", example = "30")
    private Integer duracionMinutos;

    @Schema(description = "Precio acordado del servicio", example = "25000.00")
    private BigDecimal precio;

    @Schema(description = "Código ISO de la moneda", example = "COP")
    private String moneda;

    @Schema(description = "Notas del cliente para el proveedor", example = "Corte bajo")
    private String notas;

    @Schema(description = "Resumen legible de la reserva para el cliente", example = "\"Corte clásico\", \"Barbería Centro\", \"Carlos Ruiz\", lunes 09/11/2026, 09:00-09:30")
    private String resumen;

    @Schema(description = "Fecha y hora de registro de la reserva")
    private OffsetDateTime fechaCreacion;
}
