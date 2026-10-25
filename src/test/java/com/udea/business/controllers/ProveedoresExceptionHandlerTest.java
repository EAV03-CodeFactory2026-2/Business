package com.udea.business.controllers;

import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProveedoresExceptionHandlerTest {

    private final ProveedoresExceptionHandler handler = new ProveedoresExceptionHandler();

    @Test
    void errorDeNegocioManejaAccesoDenegado() {
        ResponseEntity<Map<String, String>> response = handler.errorDeNegocio(new AccesoDenegadoException("Acceso denegado"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Acceso denegado", response.getBody().get("error"));
    }

    @Test
    void errorDeNegocioManejaRecursoNoEncontrado() {
        ResponseEntity<Map<String, String>> response = handler.errorDeNegocio(new RecursoNoEncontradoException("No encontrado"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("No encontrado", response.getBody().get("error"));
    }

    @Test
    void errorDeNegocioManejaRecursoDuplicado() {
        ResponseEntity<Map<String, String>> response = handler.errorDeNegocio(new RecursoDuplicadoException("Duplicado"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Duplicado", response.getBody().get("error"));
    }

    @Test
    void errorDeNegocioManejaSolicitudInvalida() {
        ResponseEntity<Map<String, String>> response = handler.errorDeNegocio(new SolicitudInvalidaException("Invalido"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalido", response.getBody().get("error"));
    }

    @Test
    void errorDeNegocioManejaExcepcionDesconocidaConFallback400() {
        RuntimeException exDesconocida = new RuntimeException("Error no mapeado");
        ResponseEntity<Map<String, String>> response = handler.errorDeNegocio(exDesconocida);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Error no mapeado", response.getBody().get("error"));
    }

    @Test
    void errorInesperadoRetorna500YMensajeEstandar() {
        ResponseEntity<Map<String, String>> response = handler.errorInesperado(new NullPointerException("null pointer"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Ha ocurrido un error inesperado en el servidor.", response.getBody().get("error"));
    }
}
