package com.udea.business.services;

import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.EstadoVisibleReserva;
import com.udea.business.entities.Reserva;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClasificadorReservasTest {

    private static final LocalDateTime JUEVES_05 = LocalDateTime.of(2026, 11, 5, 10, 0);

    private final ClasificadorReservas clasificador = new ClasificadorReservas();

    // HU-0019/CA-01
    @Test
    void debeOrdenarLasProximasDeLaMasCercanaALaMasLejana_cuandoHayVariasConfirmadas() {
        Reserva del12 = reserva(1, LocalDateTime.of(2026, 11, 12, 9, 0), EstadoReserva.CONFIRMADA);
        Reserva del09 = reserva(2, LocalDateTime.of(2026, 11, 9, 9, 0), EstadoReserva.CONFIRMADA);

        IClasificadorReservas.ReservasClasificadas grupos = clasificador.clasificar(List.of(del12, del09), JUEVES_05);

        assertEquals(List.of(del09, del12), grupos.proximas());
        assertTrue(grupos.historial().isEmpty());
    }

    // HU-0019/CA-02
    @Test
    void debeMarcarComoRealizada_cuandoLaHoraDeInicioYaPaso() {
        Reserva del02 = reserva(1, LocalDateTime.of(2026, 11, 2, 9, 0), EstadoReserva.CONFIRMADA);
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 2, 9, 31);

        IClasificadorReservas.ReservasClasificadas grupos = clasificador.clasificar(List.of(del02), ahora);

        assertEquals(List.of(del02), grupos.historial());
        assertTrue(grupos.proximas().isEmpty());
        assertEquals(EstadoVisibleReserva.REALIZADA, clasificador.estadoVisible(del02, ahora));
    }

    // HU-0019/CA-03
    @Test
    void debeEnviarAlHistorialComoCancelada_cuandoLaReservaFuturaFueCancelada() {
        Reserva del10 = reserva(1, LocalDateTime.of(2026, 11, 10, 9, 0), EstadoReserva.CANCELADA);

        IClasificadorReservas.ReservasClasificadas grupos = clasificador.clasificar(List.of(del10), JUEVES_05);

        assertEquals(List.of(del10), grupos.historial());
        assertTrue(grupos.proximas().isEmpty());
        assertEquals(EstadoVisibleReserva.CANCELADA, clasificador.estadoVisible(del10, JUEVES_05));
    }

    @Test
    void debeOrdenarElHistorialDeLaMasRecienteALaMasAntigua_cuandoHayRealizadasYCanceladas() {
        Reserva realizada01 = reserva(1, LocalDateTime.of(2026, 11, 1, 9, 0), EstadoReserva.CONFIRMADA);
        Reserva cancelada10 = reserva(2, LocalDateTime.of(2026, 11, 10, 9, 0), EstadoReserva.CANCELADA);
        Reserva realizada03 = reserva(3, LocalDateTime.of(2026, 11, 3, 9, 0), EstadoReserva.CONFIRMADA);
        Reserva proxima09 = reserva(4, LocalDateTime.of(2026, 11, 9, 9, 0), EstadoReserva.CONFIRMADA);

        IClasificadorReservas.ReservasClasificadas grupos = clasificador.clasificar(
                List.of(realizada01, cancelada10, realizada03, proxima09), JUEVES_05);

        assertEquals(List.of(cancelada10, realizada03, realizada01), grupos.historial());
        assertEquals(List.of(proxima09), grupos.proximas());
    }

    @Test
    void debeMostrarConfirmada_cuandoLaReservaAunNoInicia() {
        Reserva futura = reserva(1, JUEVES_05.plusMinutes(1), EstadoReserva.CONFIRMADA);

        assertEquals(EstadoVisibleReserva.CONFIRMADA, clasificador.estadoVisible(futura, JUEVES_05));
    }

    @Test
    void debeMostrarRealizada_cuandoLaReservaIniciaJustoAhora() {
        Reserva ahora = reserva(1, JUEVES_05, EstadoReserva.CONFIRMADA);

        assertEquals(EstadoVisibleReserva.REALIZADA, clasificador.estadoVisible(ahora, JUEVES_05));
    }

    @Test
    void debeDevolverGruposVacios_cuandoNoHayReservas() {
        IClasificadorReservas.ReservasClasificadas grupos = clasificador.clasificar(List.of(), JUEVES_05);

        assertTrue(grupos.proximas().isEmpty());
        assertTrue(grupos.historial().isEmpty());
    }

    private Reserva reserva(Integer id, LocalDateTime inicio, EstadoReserva estado) {
        return Reserva.builder()
                .idReserva(id)
                .inicio(inicio)
                .fin(inicio.plusMinutes(30))
                .estado(estado)
                .build();
    }
}
