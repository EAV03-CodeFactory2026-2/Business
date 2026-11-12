package com.udea.business.services.acceso;

import com.udea.business.entities.Reserva;
import com.udea.business.security.IUsuarioActual;
import org.springframework.stereotype.Component;

/** El cliente solo ve las reservas que él mismo hizo. */
@Component
public class ReglaAccesoCliente implements IReglaAccesoReserva {

    @Override
    public boolean permite(Reserva reserva, IUsuarioActual usuario) {
        return usuario.esCliente() && usuario.getId().equals(reserva.getClienteId());
    }
}
