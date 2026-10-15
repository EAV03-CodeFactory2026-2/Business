package com.udea.business.security;

import com.udea.business.exceptions.AccesoDenegadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual implements IUsuarioActual {

    private static final String ROL_PROPIETARIO = "ROLE_PROPIETARIO";

    @Override
    public Integer getId() {
        Authentication auth = autenticacion();
        try {
            return Integer.parseInt(auth.getName());
        } catch (NumberFormatException e) {
            throw new AccesoDenegadoException("No fue posible identificar al usuario autenticado.");
        }
    }

    @Override
    public boolean esPropietario() {
        // JwtAuthenticationFilter solo concede el rol activo del token, así que esto respeta el rol con que se opera.
        return autenticacion().getAuthorities().stream()
                .anyMatch(a -> ROL_PROPIETARIO.equalsIgnoreCase(a.getAuthority()));
    }

    private Authentication autenticacion() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccesoDenegadoException("No fue posible identificar al usuario autenticado.");
        }
        return auth;
    }
}
