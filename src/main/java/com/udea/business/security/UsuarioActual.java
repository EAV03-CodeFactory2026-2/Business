package com.udea.business.security;

import com.udea.business.exceptions.AccesoDenegadoException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual implements IUsuarioActual {

    private static final String ROL_PROPIETARIO = "ROLE_PROPIETARIO";
    private static final String ROL_PROVEEDOR = "ROLE_PROVEEDOR";
    private static final String MSG_NO_IDENTIFICADO = "No fue posible identificar al usuario autenticado.";

    @Override
    public Integer getId() {
        try {
            return Integer.valueOf(autenticacionValida().getName());
        } catch (NumberFormatException e) {
            throw new AccesoDenegadoException(MSG_NO_IDENTIFICADO);
        }
    }

    @Override
    public boolean esPropietario() {
        return tieneRol(ROL_PROPIETARIO);
    }

    @Override
    public boolean esProveedor() {
        return tieneRol(ROL_PROVEEDOR);
    }

    private boolean tieneRol(String rol) {
        for (GrantedAuthority ga : autenticacionValida().getAuthorities()) {
            if (rol.equalsIgnoreCase(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private Authentication autenticacionValida() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccesoDenegadoException(MSG_NO_IDENTIFICADO);
        }
        return auth;
    }
}
