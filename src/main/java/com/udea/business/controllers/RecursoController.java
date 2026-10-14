package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.RecursoCreateRequest;
import com.udea.business.controllers.DTOs.RecursoResponse;
import com.udea.business.entities.Recurso;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.services.IRecursoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/negocios/{negocioId}/recursos")
@RequiredArgsConstructor
@Tag(name = "Recursos", description = "Inventario de recursos físicos (espacios y equipos) de un negocio")
public class RecursoController {

    public static final String MSG_REGISTRADO = "Recurso registrado correctamente.";
    public static final String MSG_LISTA_VACIA = "Este negocio aún no tiene recursos registrados.";
    public static final String MSG_LISTA = "Recursos consultados correctamente.";

    private final IRecursoService recursoService;

    @Operation(
            summary = "Registrar un recurso físico",
            description = "Registra un espacio o equipo en un negocio del propietario autenticado. "
                    + "El nombre es único por negocio (sin distinguir mayúsculas ni tildes)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Recurso registrado correctamente con estado Activo."),
            @ApiResponse(responseCode = "400", description = "Datos inválidos (nombre, tipo, cantidad o descripción)."),
            @ApiResponse(responseCode = "403", description = "El rol activo no es Propietario o el negocio no le pertenece."),
            @ApiResponse(responseCode = "404", description = "El negocio no existe."),
            @ApiResponse(responseCode = "409", description = "Ya existe un recurso con este nombre en el negocio.")
    })
    @PostMapping
    public ResponseEntity<?> registrarRecurso(@PathVariable("negocioId") Integer negocioId,
                                              @RequestBody RecursoCreateRequest request,
                                              Principal principal) {
        try {
            Integer usuarioId = Integer.parseInt(principal.getName());
            Recurso recurso = recursoService.registrarRecurso(negocioId, request, usuarioId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "mensaje", MSG_REGISTRADO,
                            "recurso", RecursoResponse.desde(recurso)
                    ));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @Operation(
            summary = "Listar recursos de un negocio",
            description = "Retorna los recursos del negocio indicado. Si no tiene, retorna una lista vacía."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listado retornado (puede estar vacío)."),
            @ApiResponse(responseCode = "403", description = "El rol activo no es Propietario o el negocio no le pertenece."),
            @ApiResponse(responseCode = "404", description = "El negocio no existe.")
    })
    @GetMapping
    public ResponseEntity<?> listarRecursos(@PathVariable("negocioId") Integer negocioId, Principal principal) {
        try {
            Integer usuarioId = Integer.parseInt(principal.getName());
            List<RecursoResponse> recursos = recursoService.listarRecursos(negocioId, usuarioId).stream()
                    .map(RecursoResponse::desde)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "mensaje", recursos.isEmpty() ? MSG_LISTA_VACIA : MSG_LISTA,
                    "recursos", recursos
            ));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    private ResponseEntity<Map<String, String>> error(RuntimeException e) {
        HttpStatus status = switch (e) {
            case AccesoDenegadoException ignored -> HttpStatus.FORBIDDEN;
            case RecursoNoEncontradoException ignored -> HttpStatus.NOT_FOUND;
            case RecursoDuplicadoException ignored -> HttpStatus.CONFLICT;
            case IllegalArgumentException ignored -> HttpStatus.BAD_REQUEST;
            default -> throw e;
        };
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }
}
