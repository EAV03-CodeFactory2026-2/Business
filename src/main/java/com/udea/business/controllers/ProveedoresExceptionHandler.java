package com.udea.business.controllers;

import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Traduce las excepciones de la asignación de proveedores a respuestas HTTP.
 * Se limita a ese controlador para no cambiar el comportamiento de los existentes.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {AsignacionProveedorController.class, MisServiciosController.class})
public class ProveedoresExceptionHandler {

    private static final Map<Class<? extends Exception>, HttpStatus> ESTADOS = Map.of(
            AccesoDenegadoException.class, HttpStatus.FORBIDDEN,
            RecursoNoEncontradoException.class, HttpStatus.NOT_FOUND,
            RecursoDuplicadoException.class, HttpStatus.CONFLICT,
            SolicitudInvalidaException.class, HttpStatus.BAD_REQUEST
    );

    @ExceptionHandler({
            AccesoDenegadoException.class,
            RecursoNoEncontradoException.class,
            RecursoDuplicadoException.class,
            SolicitudInvalidaException.class
    })
    public ResponseEntity<Map<String, String>> errorDeNegocio(RuntimeException e) {
        HttpStatus status = ESTADOS.getOrDefault(e.getClass(), HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "El parámetro '" + e.getName() + "' tiene un formato inválido."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoInvalido() {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "El cuerpo de la solicitud no tiene un formato válido."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> errorInesperado(Exception e) {
        log.error("Error inesperado en el servidor: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Ha ocurrido un error inesperado en el servidor."));
    }
}
