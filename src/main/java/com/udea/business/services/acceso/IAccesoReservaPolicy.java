package com.udea.business.services.acceso;

import com.udea.business.entities.Reserva;

/**
 * Política de visibilidad de una reserva para el usuario autenticado.
 */
public interface IAccesoReservaPolicy {

    boolean puedeVer(Reserva reserva);
}
