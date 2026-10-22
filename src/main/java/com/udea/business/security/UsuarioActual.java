package com.udea.business.security;

import com.udea.business.exceptions.AccesoDenegadoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UsuarioActual implements IUsuarioActual {

    private static final String ROL_PROPIETARIO = "ROLE_PROPIETARIO";
    private static final String MSG_NO_IDENTIFICADO = "No fue posible identificar al usuario autenticado.";

    @Override
    public Integer getId() {
        try {
            return Integer.valueOf(Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                    .map(Authentication::getName)
                    .orElse(""));
        } catch (NumberFormatException e) {
            throw new AccesoDenegadoException(MSG_NO_IDENTIFICADO);
        }
    }

    @Override
    public boolean esPropietario() {
        // JwtAuthenticationFilter solo concede el rol activo del token, así que esto respeta el rol con que se opera.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccesoDenegadoException(MSG_NO_IDENTIFICADO);
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROL_PROPIETARIO::equalsIgnoreCase);
    }
}
