package com.udea.business.exceptions;

/**
 * Datos de entrada que no cumplen las reglas de negocio (HTTP 400).
 */
public final class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String message) {
        super(message);
    }
}
