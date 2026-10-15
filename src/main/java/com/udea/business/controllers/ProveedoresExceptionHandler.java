package com.udea.business.controllers;

import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
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
@RestControllerAdvice(assignableTypes = AsignacionProveedorController.class)
public class ProveedoresExceptionHandler {

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, String>> accesoDenegado(AccesoDenegadoException e) {
        return error(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(RecursoNoEncontradoException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<Map<String, String>> duplicado(RecursoDuplicadoException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, String>> solicitudInvalida(SolicitudInvalidaException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "El parámetro '" + e.getName() + "' tiene un formato inválido.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoInvalido(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud no tiene un formato válido.");
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
