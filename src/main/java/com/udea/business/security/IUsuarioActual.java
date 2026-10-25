package com.udea.business.security;

/**
 * Datos del usuario autenticado en la petición actual (extraídos del JWT).
 */
public interface IUsuarioActual {
    Integer getId();
    boolean esPropietario();
    boolean esProveedor();
}
