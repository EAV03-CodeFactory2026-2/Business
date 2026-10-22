package com.udea.business.entities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Estados de un servicio según cuántos proveedores lo prestan.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class EstadoServicio {

    // Se conserva "No Asignado" porque es el valor que ya usan la BD y el registro de servicios.
    public static final String NO_ASIGNADO = "No Asignado";
    public static final String ASIGNADO = "Asignado";

    public static String segunProveedores(long cantidadProveedores) {
        return cantidadProveedores > 0 ? ASIGNADO : NO_ASIGNADO;
    }
}
