package com.udea.business.services;

import com.udea.business.controllers.DTOs.CancelarReservaRequest;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.Reserva;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.security.IUsuarioActual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import static com.udea.business.services.CancelacionReservaValidator.MSG_CONFIRMACION_REQUERIDA;
import static com.udea.business.services.CancelacionReservaValidator.MSG_FUERA_DE_PLAZO;
import static com.udea.business.services.CancelacionReservaValidator.MSG_HORA_INICIO_PASADA;
import static com.udea.business.services.CancelacionReservaValidator.MSG_MOTIVO_DEMASIADO_LARGO;
import static com.udea.business.services.CancelacionReservaValidator.MSG_RESERVA_YA_CANCELADA;
import static com.udea.business.services.CancelacionReservaValidator.MSG_SIN_PERMISO;
import static com.udea.business.services.CancelacionReservaValidator.MSG_SOLO_CLIENTES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelacionReservaValidatorTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Integer LAURA_ID = 20;
    private static final Integer MARIO_ID = 21;
    private static final LocalDate DOMINGO = LocalDate.of(2026, 11, 8);
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    private static final LocalDateTime INICIO = LocalDateTime.of(LUNES, LocalTime.of(9, 0));

    @Mock
    private IUsuarioActual laura;

    private final CancelacionReservaValidator validator = conReloj(LocalDateTime.of(DOMINGO, LocalTime.of(18, 0)));

    // HU-0021/CA-01
    @Test
    void debePermitirCancelar_cuandoFaltanMasDeDosHorasYLaReservaEsPropia() {
        comoLaura();

        assertDoesNotThrow(() -> validator.validarCancelacion(reservaDe(LAURA_ID), laura,
                confirmada("Viaje de trabajo"), LocalDateTime.of(DOMINGO, LocalTime.of(18, 0))));
    }

    // HU-0021/CA-03
    @Test
    void debeRechazarLaCancelacion_cuandoFaltanMenosDeDosHoras() {
        comoLaura();

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarCancelacion(reservaDe(LAURA_ID), laura, confirmada(null),
                        LocalDateTime.of(LUNES, LocalTime.of(7, 30))));

        assertEquals(MSG_FUERA_DE_PLAZO, ex.getMessage());
    }

    // HU-0021/CA-04
    @Test
    void debePermitirCancelar_cuandoFaltanExactamenteDosHoras() {
        comoLaura();

        assertDoesNotThrow(() -> validator.validarCancelacion(reservaDe(LAURA_ID), laura, confirmada(null),
                LocalDateTime.of(LUNES, LocalTime.of(7, 0))));
    }

    // HU-0021/CA-04
    @Test
    void debeRechazarLaCancelacion_cuandoHaPasadoUnMinutoDelLimite() {
        comoLaura();

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarCancelacion(reservaDe(LAURA_ID), laura, confirmada(null),
                        LocalDateTime.of(LUNES, LocalTime.of(7, 1))));

        assertEquals(MSG_FUERA_DE_PLAZO, ex.getMessage());
    }

    @Test
    void debeInformarFueraDePlazo_cuandoEsJustoLaHoraDeInicio() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarPlazoCancelacion(reservaDe(LAURA_ID), INICIO));

        assertEquals(MSG_FUERA_DE_PLAZO, ex.getMessage());
    }

    @Test
    void debeRechazarLaCancelacion_cuandoLaHoraDeInicioYaPaso() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarPlazoCancelacion(reservaDe(LAURA_ID), INICIO.plusMinutes(1)));

        assertEquals(MSG_HORA_INICIO_PASADA, ex.getMessage());
    }

    @Test
    void debeUsarElRelojDelSistema_cuandoNoSeIndicaLaHoraActual() {
        CancelacionReservaValidator aLas730 = conReloj(LocalDateTime.of(LUNES, LocalTime.of(7, 30)));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> aLas730.validarPlazoCancelacion(reservaDe(LAURA_ID), null));

        assertEquals(MSG_FUERA_DE_PLAZO, ex.getMessage());
    }

    @Test
    void debeOmitirElPlazo_cuandoLaReservaNoTieneInicio() {
        Reserva sinInicio = reservaDe(LAURA_ID);
        sinInicio.setInicio(null);

        assertDoesNotThrow(() -> validator.validarPlazoCancelacion(sinInicio, LocalDateTime.of(LUNES, LocalTime.of(8, 0))));
        assertDoesNotThrow(() -> validator.validarPlazoCancelacion(null, LocalDateTime.of(LUNES, LocalTime.of(8, 0))));
    }

    // HU-0021/CA-05
    @Test
    void debeInformarQueYaFueCancelada_cuandoSeIntentaCancelarDeNuevo() {
        comoLaura();
        Reserva cancelada = reservaDe(LAURA_ID);
        cancelada.setEstado(EstadoReserva.CANCELADA);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarCancelacion(cancelada, laura, confirmada(null),
                        LocalDateTime.of(DOMINGO, LocalTime.of(18, 0))));

        assertEquals(MSG_RESERVA_YA_CANCELADA, ex.getMessage());
    }

    @Test
    void debeAceptarElEstado_cuandoNoHayReserva() {
        assertDoesNotThrow(() -> validator.validarEstadoReserva(null));
    }

    // HU-0021/CA-06
    @Test
    void debeRechazarLaCancelacion_cuandoLaReservaEsDeOtroCliente() {
        comoLaura();

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> validator.validarCancelacion(reservaDe(MARIO_ID), laura, confirmada(null),
                        LocalDateTime.of(DOMINGO, LocalTime.of(18, 0))));

        assertEquals(MSG_SIN_PERMISO, ex.getMessage());
    }

    @Test
    void debeRechazarLaCancelacion_cuandoLaReservaNoTieneCliente() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> validator.validarPropiedadReserva(reservaDe(null), laura));

        assertEquals(MSG_SIN_PERMISO, ex.getMessage());
    }

    @Test
    void debeRechazarLaCancelacion_cuandoNoHayReservaOUsuario() {
        AccesoDenegadoException sinReserva = assertThrows(AccesoDenegadoException.class,
                () -> validator.validarPropiedadReserva(null, laura));
        AccesoDenegadoException sinUsuario = assertThrows(AccesoDenegadoException.class,
                () -> validator.validarPropiedadReserva(reservaDe(LAURA_ID), null));

        assertEquals(MSG_SIN_PERMISO, sinReserva.getMessage());
        assertEquals(MSG_SIN_PERMISO, sinUsuario.getMessage());
    }

    @Test
    void debeRechazarLaCancelacion_cuandoElUsuarioNoEsCliente() {
        when(laura.esCliente()).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> validator.validarCancelacion(reservaDe(LAURA_ID), laura, confirmada(null),
                        LocalDateTime.of(DOMINGO, LocalTime.of(18, 0))));

        assertEquals(MSG_SOLO_CLIENTES, ex.getMessage());
    }

    @Test
    void debeRechazarLaCancelacion_cuandoNoHayUsuarioAutenticado() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> validator.validarRolCliente(null));

        assertEquals(MSG_SOLO_CLIENTES, ex.getMessage());
    }

    @Test
    void debePedirConfirmacion_cuandoLaSolicitudNoTraeCuerpo() {
        comoLaura();

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarCancelacion(reservaDe(LAURA_ID), laura, null,
                        LocalDateTime.of(DOMINGO, LocalTime.of(18, 0))));

        assertEquals(MSG_CONFIRMACION_REQUERIDA, ex.getMessage());
    }

    @Test
    void debePedirConfirmacion_cuandoElClienteNoConfirmaLaAccion() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarConfirmacion(false));

        assertEquals(MSG_CONFIRMACION_REQUERIDA, ex.getMessage());
    }

    @Test
    void debeRechazarElMotivo_cuandoSupera250Caracteres() {
        comoLaura();
        CancelarReservaRequest request = confirmada("a".repeat(251));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarCancelacion(reservaDe(LAURA_ID), laura, request,
                        LocalDateTime.of(DOMINGO, LocalTime.of(18, 0))));

        assertEquals(MSG_MOTIVO_DEMASIADO_LARGO, ex.getMessage());
    }

    @Test
    void debeAceptarElMotivo_cuandoTieneExactamente250Caracteres() {
        assertDoesNotThrow(() -> validator.validarMotivo("a".repeat(250)));
    }

    // HU-0021/CA-01
    @Test
    void debeValidarConElReloj_cuandoSeUsaLaSobrecargaSinHoraActual() {
        comoLaura();

        assertDoesNotThrow(() -> validator.validarCancelacion(reservaDe(LAURA_ID), laura, confirmada(null)));
    }

    @Test
    void debeRechazarPorPlazoConElReloj_cuandoSeUsaLaSobrecargaSinHoraActual() {
        comoLaura();
        CancelacionReservaValidator aLas730 = conReloj(LocalDateTime.of(LUNES, LocalTime.of(7, 30)));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> aLas730.validarCancelacion(reservaDe(LAURA_ID), laura, confirmada(null)));

        assertEquals(MSG_FUERA_DE_PLAZO, ex.getMessage());
    }

    private void comoLaura() {
        when(laura.esCliente()).thenReturn(true);
        when(laura.getId()).thenReturn(LAURA_ID);
    }

    private CancelacionReservaValidator conReloj(LocalDateTime ahora) {
        return new CancelacionReservaValidator(Clock.fixed(ahora.atZone(ZONA).toInstant(), ZONA));
    }

    private CancelarReservaRequest confirmada(String motivo) {
        return CancelarReservaRequest.builder().motivo(motivo).confirmacion(true).build();
    }

    private Reserva reservaDe(Integer clienteId) {
        return Reserva.builder()
                .idReserva(100)
                .clienteId(clienteId)
                .proveedorId(12)
                .servicioId(1)
                .inicio(INICIO)
                .fin(INICIO.plusMinutes(30))
                .estado(EstadoReserva.CONFIRMADA)
                .build();
    }
}
