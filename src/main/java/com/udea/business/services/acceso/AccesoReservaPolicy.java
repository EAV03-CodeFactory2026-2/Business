package com.udea.business.services.acceso;

import com.udea.business.entities.Reserva;
import com.udea.business.security.IUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Permite ver la reserva si alguna de las reglas de rol lo autoriza
 * (un usuario puede tener varios roles a la vez).
 */
@Component
@RequiredArgsConstructor
public class AccesoReservaPolicy implements IAccesoReservaPolicy {

    private final List<IReglaAccesoReserva> reglas;
    private final IUsuarioActual usuarioActual;

    @Override
    public boolean puedeVer(Reserva reserva) {
        return reglas.stream().anyMatch(regla -> regla.permite(reserva, usuarioActual));
    }
}
