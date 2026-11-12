package com.udea.business.services.acceso;

import com.udea.business.entities.Negocio;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.security.IUsuarioActual;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** El propietario ve las reservas de todos los proveedores en los servicios de sus negocios. */
@Component
public class ReglaAccesoPropietario implements IReglaAccesoReserva {

    @Override
    public boolean permite(Reserva reserva, IUsuarioActual usuario) {
        if (!usuario.esPropietario()) {
            return false;
        }
        return Optional.ofNullable(reserva.getServicio())
                .map(Servicio::getNegocio)
                .map(Negocio::getPropietarioId)
                .map(usuario.getId()::equals)
                .orElse(false);
    }
}
