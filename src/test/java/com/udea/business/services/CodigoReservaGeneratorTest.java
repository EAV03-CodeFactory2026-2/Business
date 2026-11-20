package com.udea.business.services;

import com.udea.business.repositories.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodigoReservaGeneratorTest {

    private static final String FORMATO = "RSV-\\d{8}";

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private CodigoReservaGenerator generator;

    // HU-0018/CA-01
    @Test
    void debeGenerarUnCodigoConPrefijoYOchoDigitos_cuandoNoExisteOtroIgual() {
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(false);

        String codigo = generator.generarCodigo();

        assertTrue(codigo.matches(FORMATO), codigo);
        verify(reservaRepository).existsByCodigoReserva(codigo);
    }

    @Test
    void debeGenerarOtroCodigo_cuandoElPrimeroYaEstaRegistrado() {
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(true, false);

        String codigo = generator.generarCodigo();

        assertTrue(codigo.matches(FORMATO), codigo);
        verify(reservaRepository, times(2)).existsByCodigoReserva(anyString());
    }

    @Test
    void debeUsarElCodigoDeRespaldo_cuandoSeAgotanLosIntentos() {
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(true);

        String codigo = generator.generarCodigo();

        assertTrue(codigo.matches(FORMATO), codigo);
        verify(reservaRepository, times(100)).existsByCodigoReserva(anyString());
    }
}
