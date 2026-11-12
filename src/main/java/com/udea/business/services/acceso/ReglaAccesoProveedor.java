package com.udea.business.services.acceso;

import com.udea.business.entities.Reserva;
import com.udea.business.security.IUsuarioActual;
import org.springframework.stereotype.Component;

/** El proveedor solo ve las reservas en las que él presta el servicio. */
@Component
public class ReglaAccesoProveedor implements IReglaAccesoReserva {

    @Override
    public boolean permite(Reserva reserva, IUsuarioActual usuario) {
        return usuario.esProveedor() && usuario.getId().equals(reserva.getProveedorId());
    }
}
