package com.udea.business.security;

import com.udea.business.exceptions.AccesoDenegadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioActualTest {

    private static final String MSG_NO_IDENTIFICADO = "No fue posible identificar al usuario autenticado.";

    private final UsuarioActual usuarioActual = new UsuarioActual();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getIdDevuelveElIdDelUsuarioAutenticado() {
        autenticar("12", "ROLE_PROVEEDOR");

        Integer id = usuarioActual.getId();

        assertEquals(12, id);
    }

    @Test
    void getIdLanzaAccesoDenegadoCuandoElIdNoEsNumerico() {
        autenticar("ana@correo.com", "ROLE_PROPIETARIO");

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, usuarioActual::getId);

        assertEquals(MSG_NO_IDENTIFICADO, ex.getMessage());
    }

    @Test
    void getIdLanzaAccesoDenegadoCuandoNoHayAutenticacion() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, usuarioActual::getId);

        assertEquals(MSG_NO_IDENTIFICADO, ex.getMessage());
    }

    @Test
    void getIdLanzaAccesoDenegadoCuandoLaAutenticacionNoTieneNombre() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("usuario");
        when(auth.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, usuarioActual::getId);

        assertEquals(MSG_NO_IDENTIFICADO, ex.getMessage());
    }

    @Test
    void getIdLanzaAccesoDenegadoCuandoElUsuarioEsAnonimo() {
        SecurityContextHolder.getContext().setAuthentication(anonimo());

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, usuarioActual::getId);

        assertEquals(MSG_NO_IDENTIFICADO, ex.getMessage());
    }

    @Test
    void getIdLanzaAccesoDenegadoCuandoNoEstaAutenticado() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, usuarioActual::getId);

        assertEquals(MSG_NO_IDENTIFICADO, ex.getMessage());
    }

    @Test
    void esPropietarioDevuelveTrueConElRolPropietario() {
        autenticar("7", "ROLE_PROPIETARIO");

        boolean propietario = usuarioActual.esPropietario();

        assertTrue(propietario);
    }

    @Test
    void esPropietarioDevuelveTrueCuandoElPropietarioTieneVariosRoles() {
        autenticar("7", "ROLE_PROVEEDOR", "ROLE_PROPIETARIO");

        boolean propietario = usuarioActual.esPropietario();

        assertTrue(propietario);
    }

    @Test
    void esPropietarioDevuelveFalseConElRolProveedor() {
        autenticar("12", "ROLE_PROVEEDOR");

        boolean propietario = usuarioActual.esPropietario();

        assertFalse(propietario);
    }

    @Test
    void esPropietarioDevuelveFalseConElRolCliente() {
        autenticar("20", "ROLE_CLIENTE");

        boolean propietario = usuarioActual.esPropietario();

        assertFalse(propietario);
    }

    @Test
    void esPropietarioDevuelveFalseCuandoNoTieneRoles() {
        autenticar("7");

        boolean propietario = usuarioActual.esPropietario();

        assertFalse(propietario);
    }

    @Test
    void esPropietarioLanzaAccesoDenegadoCuandoElUsuarioEsAnonimo() {
        SecurityContextHolder.getContext().setAuthentication(anonimo());

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, usuarioActual::esPropietario);

        assertEquals(MSG_NO_IDENTIFICADO, ex.getMessage());
    }

    @Test
    void esPropietarioLanzaAccesoDenegadoCuandoNoHayAutenticacion() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, usuarioActual::esPropietario);

        assertEquals(MSG_NO_IDENTIFICADO, ex.getMessage());
    }

    @Test
    void esPropietarioLanzaAccesoDenegadoCuandoNoEstaAutenticado() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, usuarioActual::esPropietario);

        assertEquals(MSG_NO_IDENTIFICADO, ex.getMessage());
    }

    @Test
    void esProveedorDevuelveTrueCuandoTieneRolProveedor() {
        autenticar("12", "ROLE_PROVEEDOR");

        assertTrue(usuarioActual.esProveedor());
    }

    @Test
    void esProveedorDevuelveFalseCuandoTieneOtroRol() {
        autenticar("12", "ROLE_PROPIETARIO");

        assertFalse(usuarioActual.esProveedor());
    }

    @Test
    void esProveedorLanzaAccesoDenegadoCuandoNoHayAutenticacion() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, usuarioActual::esProveedor);

        assertEquals(MSG_NO_IDENTIFICADO, ex.getMessage());
    }

    private void autenticar(String userId, String... roles) {
        List<SimpleGrantedAuthority> authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, authorities));
    }

    private AnonymousAuthenticationToken anonimo() {
        return new AnonymousAuthenticationToken("clave", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }
}
