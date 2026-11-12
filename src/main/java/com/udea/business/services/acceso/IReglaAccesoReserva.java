package com.udea.business.services.acceso;

import com.udea.business.entities.Reserva;
import com.udea.business.security.IUsuarioActual;

/**
 * Regla que decide si el usuario autenticado, por uno de sus roles, puede ver una reserva.
 * <p>
 * Cada rol aporta su propia regla (Strategy). Para soportar un rol nuevo basta con
 * agregar otra implementación como bean, sin modificar las existentes (Open/Closed).
 */
public interface IReglaAccesoReserva {

    boolean permite(Reserva reserva, IUsuarioActual usuario);
}
