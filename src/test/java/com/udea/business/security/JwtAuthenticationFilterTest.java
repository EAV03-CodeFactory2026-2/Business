package com.udea.business.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "header.payload.firma";

    @Mock
    private IJwtService jwtService;

    @Mock
    private FilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterContinuaSinAutenticarCuandoNoHayHeaderAuthorization() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(autenticacion());
        verifyNoInteractions(jwtService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterContinuaSinAutenticarCuandoElHeaderNoEsBearer() throws Exception {
        request.addHeader("Authorization", "Basic dXN1YXJpbzpjbGF2ZQ==");

        filter.doFilter(request, response, chain);

        assertNull(autenticacion());
        verifyNoInteractions(jwtService);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterContinuaSinAutenticarCuandoElTokenEsInvalido() throws Exception {
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(autenticacion());
        verify(jwtService, never()).extractUserId(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterValidaUnTokenVacioCuandoElHeaderSoloTraeElPrefijo() throws Exception {
        request.addHeader("Authorization", "Bearer ");
        when(jwtService.isTokenValid("")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertNull(autenticacion());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterConcedeTodosLosRolesCuandoNoHayRolActivo() throws Exception {
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn("7");
        when(jwtService.extractRoles(TOKEN)).thenReturn(List.of("Propietario", "Proveedor"));
        when(jwtService.extractRolActivo(TOKEN)).thenReturn(null);

        filter.doFilter(request, response, chain);

        Authentication auth = autenticacion();
        assertTrue(auth.isAuthenticated());
        assertEquals("7", auth.getName());
        assertNull(auth.getCredentials());
        assertEquals(List.of("ROLE_PROPIETARIO", "ROLE_PROVEEDOR"), authorities(auth));
        assertInstanceOf(WebAuthenticationDetails.class, auth.getDetails());
        InOrder orden = inOrder(jwtService, chain);
        orden.verify(jwtService).isTokenValid(TOKEN);
        orden.verify(jwtService).extractUserId(TOKEN);
        orden.verify(jwtService).extractRoles(TOKEN);
        orden.verify(jwtService).extractRolActivo(TOKEN);
        orden.verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterConcedeSoloElRolActivoCuandoEstaEnLosRoles() throws Exception {
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn("7");
        when(jwtService.extractRoles(TOKEN)).thenReturn(List.of("Propietario", "Proveedor"));
        when(jwtService.extractRolActivo(TOKEN)).thenReturn("Propietario");

        filter.doFilter(request, response, chain);

        assertEquals(List.of("ROLE_PROPIETARIO"), authorities(autenticacion()));
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterAceptaElRolActivoSinImportarMayusculas() throws Exception {
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn("7");
        when(jwtService.extractRoles(TOKEN)).thenReturn(List.of("Propietario", "Proveedor"));
        when(jwtService.extractRolActivo(TOKEN)).thenReturn("PROVEEDOR");

        filter.doFilter(request, response, chain);

        assertEquals(List.of("ROLE_PROVEEDOR"), authorities(autenticacion()));
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterNoConcedeUnRolActivoQueNoEstaEnLosRoles() throws Exception {
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn("12");
        when(jwtService.extractRoles(TOKEN)).thenReturn(List.of("Proveedor"));
        when(jwtService.extractRolActivo(TOKEN)).thenReturn("Propietario");

        filter.doFilter(request, response, chain);

        assertNull(autenticacion());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterConservaTodosLosRolesCuandoElRolActivoEstaEnBlanco() throws Exception {
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn("7");
        when(jwtService.extractRoles(TOKEN)).thenReturn(List.of("Propietario", "Proveedor"));
        when(jwtService.extractRolActivo(TOKEN)).thenReturn("  ");

        filter.doFilter(request, response, chain);

        assertEquals(List.of("ROLE_PROPIETARIO", "ROLE_PROVEEDOR"), authorities(autenticacion()));
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterAutenticaSinAuthoritiesCuandoElTokenNoTraeRoles() throws Exception {
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn("20");
        when(jwtService.extractRoles(TOKEN)).thenReturn(List.of());
        when(jwtService.extractRolActivo(TOKEN)).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertNull(autenticacion());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterContinuaSinAutenticarCuandoElTokenNoTraeUsuario() throws Exception {
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertNull(autenticacion());
        verify(jwtService, never()).extractRoles(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterRespetaLaAutenticacionQueYaExisteEnElContexto() throws Exception {
        Authentication previa = new UsernamePasswordAuthenticationToken("99", null,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
        SecurityContextHolder.getContext().setAuthentication(previa);
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenReturn("7");

        filter.doFilter(request, response, chain);

        assertSame(previa, autenticacion());
        verify(jwtService, never()).extractRoles(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterContinuaSinAutenticarCuandoElServiceDeJwtFalla() throws Exception {
        conToken();
        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(TOKEN)).thenThrow(new IllegalStateException("claims corruptos"));

        filter.doFilter(request, response, chain);

        assertNull(autenticacion());
        verify(chain).doFilter(request, response);
    }

    private void conToken() {
        request.addHeader("Authorization", "Bearer " + TOKEN);
    }

    private Authentication autenticacion() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private List<String> authorities(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
