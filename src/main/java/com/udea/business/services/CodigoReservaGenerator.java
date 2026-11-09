package com.udea.business.services;

import com.udea.business.repositories.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class CodigoReservaGenerator implements ICodigoReservaGenerator {

    private static final String PREFIJO = "RSV-";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ReservaRepository reservaRepository;

    @Override
    public String generarCodigo() {
        String codigo;
        int intentos = 0;
        do {
            int numero = RANDOM.nextInt(100_000_000);
            codigo = PREFIJO + String.format("%08d", numero);
            intentos++;
            if (intentos > 100) {
                codigo = PREFIJO + String.format("%08d", System.currentTimeMillis() % 100_000_000);
                break;
            }
        } while (reservaRepository.existsByCodigoReserva(codigo));

        return codigo;
    }
}
