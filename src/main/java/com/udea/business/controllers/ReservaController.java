package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.CancelarReservaRequest;
import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.controllers.DTOs.FranjaDisponibleResponse;
import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.services.IDisponibilidadService;
import com.udea.business.services.IReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Reservas de Servicio", description = "Endpoints para consultar franjas horarias disponibles y gestionar citas")
public class ReservaController {

    private final IReservaService reservaService;
    private final IDisponibilidadService disponibilidadService;

    @Operation(
            summary = "Reservar una franja disponible de un servicio",
            description = "Permite a un cliente autenticado reservar una franja horaria definida y disponible. "
                    + "Garantiza que la franja no se duplique ante peticiones concurrentes y que los horarios no se crucen.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva creada exitosamente en estado Confirmada."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos, horario no disponible, franja eliminada o notas demasiado largas."),
            @ApiResponse(responseCode = "403", description = "Solo los clientes pueden realizar reservas."),
            @ApiResponse(responseCode = "404", description = "Servicio o proveedor no encontrado."),
            @ApiResponse(responseCode = "409", description = "La franja acaba de ser tomada por otro cliente o genera conflicto.")
    })
    @PostMapping({"/reservas"})
    public ResponseEntity<ReservaResponse> crearReserva(
            @PathVariable(value = "servicioId", required = false) Integer servicioIdParam,
            @RequestBody CrearReservaRequest request) {
        if (servicioIdParam != null && request != null && request.getServicioId() == null) {
            request.setServicioId(servicioIdParam);
        }
        ReservaResponse respuesta = reservaService.crearReserva(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @Operation(
            summary = "Cancelar una reserva propia",
            description = "Endpoint para cancelar una reserva propia",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tu reserva fue cancelada."),
            @ApiResponse(responseCode = "400", description = "Cancelación fuera del plazo, hora ya pasada o ya cancelada."),
            @ApiResponse(responseCode = "403", description = "No tienes permiso para cancelar esta reserva."),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada.")
    })
    @PostMapping("/reservas/{idReserva}/cancelar")
    public ResponseEntity<ReservaResponse> cancelarReservaPost(
            @PathVariable("idReserva") Integer idReserva,
            @RequestBody CancelarReservaRequest request) {
        ReservaResponse respuesta = reservaService.cancelarReserva(idReserva, request);
        return ResponseEntity.ok(respuesta);
    }

    @Operation(
            summary = "Consultar franjas horarias disponibles",
            description = "Devuelve las franjas horarias definidas y disponibles para un servicio y fecha específicos, "
                    + "excluyendo automáticamente las que ya tengan una reserva confirmada con el proveedor.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado de franjas horarias disponibles retornado."),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos o fecha faltante."),
            @ApiResponse(responseCode = "404", description = "Servicio no encontrado.")
    })
    @GetMapping({"/servicios/{servicioId}/franjas-disponibles"})
    public ResponseEntity<List<FranjaDisponibleResponse>> consultarFranjasDisponibles(
            @PathVariable("servicioId") Integer servicioId,
            @Parameter(description = "Fecha de la cita (formato YYYY-MM-DD)", required = true, example = "2026-11-09")
            @RequestParam("fecha") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Identificador opcional del proveedor", example = "12")
            @RequestParam(value = "proveedorId", required = false) Integer proveedorId) {
        List<FranjaDisponibleResponse> disponibles = disponibilidadService.consultarFranjasDisponibles(servicioId, fecha, proveedorId);
        return ResponseEntity.ok(disponibles);
    }

    @Operation(
            summary = "Consultar detalle de una reserva por su ID",
            description = "Obtiene los detalles completos y snapshot histórico de una reserva por su ID numérico. "
                    + "El cliente solo ve las suyas, el proveedor solo las que atiende y el propietario "
                    + "las de todos los proveedores de sus negocios.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle de la reserva obtenido exitosamente."),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada o sin permiso para verla.")
    })
    @GetMapping("/reservas/{idReserva}")
    public ResponseEntity<ReservaResponse> consultarPorId(@PathVariable("idReserva") Integer idReserva) {
        ReservaResponse respuesta = reservaService.consultarPorId(idReserva);
        return ResponseEntity.ok(respuesta);
    }

    @Operation(
            summary = "Consultar reserva por código de referencia",
            description = "Permite buscar una reserva mediante su código alfanumérico único (ej. RSV-00000001). "
                    + "Aplica las mismas reglas de visibilidad que la consulta por ID.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva encontrada exitosamente."),
            @ApiResponse(responseCode = "404", description = "Reserva no encontrada o sin permiso para verla.")
    })
    @GetMapping("/reservas/codigo/{codigoReserva}")
    public ResponseEntity<ReservaResponse> consultarPorCodigo(@PathVariable("codigoReserva") String codigoReserva) {
        ReservaResponse respuesta = reservaService.consultarPorCodigo(codigoReserva);
        return ResponseEntity.ok(respuesta);
    }
}
