package com.udea.business.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextoNormalizadorTest {

    @ParameterizedTest
    @ValueSource(strings = {"Camilla de masajes", "camilla de masajes", "CAMILLA DE MASAJES",
            "Camilla De Masajes", "Cámilla dé masajés", "  Camilla   de\tmasajes  "})
    void normalizaMayusculasTildesYEspacios(String texto) {
        assertEquals("camilla de masajes", TextoNormalizador.normalizar(texto));
    }

    @Test
    void conservaLaEnieComoN() {
        assertEquals("bano de espanol", TextoNormalizador.normalizar("Baño de Español"));
    }

    @Test
    void retornaNuloSiElTextoEsNulo() {
        assertNull(TextoNormalizador.normalizar(null));
    }
}
