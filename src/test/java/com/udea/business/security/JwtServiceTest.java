package com.udea.business.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRETO = "c2VjcmV0by1wcnVlYmFzLWJ1c2luZXNzLWhzMjU2ISE=";
    private static final String OTRO_SECRETO = "b3Ryby1zZWNyZXRvLWFqZW5vLWFsLWJ1c2luZXNzISE=";

    private final JwtService jwtService = new JwtService(SECRETO);

    @Test
    void extractUserIdDevuelveElSubjectDelToken() {
        String token = firmar(tokenVigente(), SECRETO);

        String userId = jwtService.extractUserId(token);

        assertEquals("12", userId);
    }

    @Test
    void extractEmailDevuelveElCorreoDelToken() {
        String token = firmar(tokenVigente().claim("email", "ana@correo.com"), SECRETO);

        String email = jwtService.extractEmail(token);

        assertEquals("ana@correo.com", email);
    }

    @Test
    void extractRolesDevuelveLosRolesDelToken() {
        String token = firmar(tokenVigente().claim("roles", List.of("Propietario", "Proveedor")), SECRETO);

        List<String> roles = jwtService.extractRoles(token);

        assertEquals(List.of("Propietario", "Proveedor"), roles);
    }

    @Test
    void extractRolActivoDevuelveElRolActivoDelToken() {
        String token = firmar(tokenVigente().claim("rolActivo", "Propietario"), SECRETO);

        String rolActivo = jwtService.extractRolActivo(token);

        assertEquals("Propietario", rolActivo);
    }

    @Test
    void extractRolesDevuelveListaVaciaCuandoElTokenNoTieneRoles() {
        String token = firmar(tokenVigente(), SECRETO);

        List<String> roles = jwtService.extractRoles(token);

        assertTrue(roles.isEmpty());
    }

    @Test
    void extractRolActivoYEmailDevuelvenNullCuandoElTokenNoTraeEsosClaims() {
        String token = firmar(tokenVigente(), SECRETO);

        String rolActivo = jwtService.extractRolActivo(token);
        String email = jwtService.extractEmail(token);

        assertNull(rolActivo);
        assertNull(email);
    }

    @Test
    void isTokenValidAceptaUnTokenVigente() {
        String token = firmar(tokenVigente(), SECRETO);

        boolean valido = jwtService.isTokenValid(token);

        assertTrue(valido);
    }

    @Test
    void isTokenValidRechazaUnTokenExpirado() {
        String token = firmar(tokenExpirado(), SECRETO);

        boolean valido = jwtService.isTokenValid(token);

        assertFalse(valido);
    }

    @Test
    void extractUserIdLanzaExpiredJwtExceptionConUnTokenExpirado() {
        String token = firmar(tokenExpirado(), SECRETO);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUserId(token));
    }

    @Test
    void isTokenValidRechazaUnTokenFirmadoConOtroSecreto() {
        String token = firmar(tokenVigente(), OTRO_SECRETO);

        boolean valido = jwtService.isTokenValid(token);

        assertFalse(valido);
    }

    @Test
    void extractUserIdLanzaSignatureExceptionConUnTokenFirmadoConOtroSecreto() {
        String token = firmar(tokenVigente(), OTRO_SECRETO);

        assertThrows(SignatureException.class, () -> jwtService.extractUserId(token));
    }

    @Test
    void isTokenValidRechazaUnTokenSinFirma() {
        String token = tokenVigente().compact();

        boolean valido = jwtService.isTokenValid(token);

        assertFalse(valido);
    }

    @Test
    void isTokenValidRechazaUnTokenSinExpiracion() {
        String token = firmar(Jwts.builder().subject("12"), SECRETO);

        boolean valido = jwtService.isTokenValid(token);

        assertFalse(valido);
    }

    @Test
    void isTokenValidRechazaUnTokenMalformado() {
        boolean valido = jwtService.isTokenValid("abc.def");

        assertFalse(valido);
    }

    @Test
    void extractUserIdLanzaMalformedJwtExceptionConUnTokenMalformado() {
        assertThrows(MalformedJwtException.class, () -> jwtService.extractUserId("abc.def"));
    }

    @Test
    void isTokenValidRechazaUnTokenVacio() {
        boolean valido = jwtService.isTokenValid("");

        assertFalse(valido);
    }

    @Test
    void isTokenValidRechazaUnTokenNulo() {
        boolean valido = jwtService.isTokenValid(null);

        assertFalse(valido);
    }

    private JwtBuilder tokenVigente() {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject("12")
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(30, ChronoUnit.MINUTES)));
    }

    private JwtBuilder tokenExpirado() {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject("12")
                .issuedAt(Date.from(ahora.minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(ahora.minus(1, ChronoUnit.HOURS)));
    }

    private String firmar(JwtBuilder builder, String secreto) {
        return builder.signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto))).compact();
    }
}
