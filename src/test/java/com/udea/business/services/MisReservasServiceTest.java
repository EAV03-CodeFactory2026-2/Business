package com.udea.business.services;

import com.udea.business.controllers.DTOs.MisReservasResponse;
import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.controllers.DTOs.ReservaResumenResponse;
import com.udea.business.entities.CancelacionReserva;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.PerfilProveedor;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.UsuarioPlataforma;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.security.IUsuarioActual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.udea.business.services.MisReservasService.MSG_ID_OBLIGATORIO;
import static com.udea.business.services.MisReservasService.MSG_RESERVA_NO_ENCONTRADA;
import static com.udea.business.services.MisReservasService.MSG_SIN_RESERVAS;
import static com.udea.business.services.MisReservasService.MSG_SOLO_CLIENTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MisReservasServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Integer LAURA_ID = 20;
    private static final LocalDateTime JUEVES_05 = LocalDateTime.of(2026, 11, 5, 10, 0);

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private IUsuarioActual usuarioActual;

    // HU-0019/CA-01
    @Test
    void debeMostrarPrimeroLaReservaDel09_cuandoTieneConfirmadasEl12YEl09() {
        comoLaura();
        when(reservaRepository.findDetalladasPorCliente(LAURA_ID)).thenReturn(List.of(
                reserva(1, LocalDateTime.of(2026, 11, 12, 9, 0), EstadoReserva.CONFIRMADA),
                reserva(2, LocalDateTime.of(2026, 11, 9, 9, 0), EstadoReserva.CONFIRMADA)));

        MisReservasResponse respuesta = servicioConReloj(JUEVES_05).consultarMisReservas();

        assertEquals(List.of("2026-11-09", "2026-11-12"),
                respuesta.getProximas().stream().map(ReservaResumenResponse::getFecha).toList());
        assertTrue(respuesta.getProximas().stream().allMatch(r -> "Confirmada".equals(r.getEstado())));
        assertTrue(respuesta.getHistorial().isEmpty());
        assertNull(respuesta.getMensaje());
    }

    // HU-0019/CA-02
    @Test
    void debeMostrarLaReservaEnHistorialComoRealizada_cuandoYaPasoSuHoraDeInicio() {
        comoLaura();
        when(reservaRepository.findDetalladasPorCliente(LAURA_ID)).thenReturn(List.of(
                reserva(1, LocalDateTime.of(2026, 11, 2, 9, 0), EstadoReserva.CONFIRMADA)));

        MisReservasResponse respuesta = servicioConReloj(LocalDateTime.of(2026, 11, 2, 9, 31)).consultarMisReservas();

        assertTrue(respuesta.getProximas().isEmpty());
        assertEquals(1, respuesta.getHistorial().size());
        assertEquals("Realizada", respuesta.getHistorial().get(0).getEstado());
    }

    // HU-0019/CA-03
    @Test
    void debeMostrarLaReservaCanceladaSoloEnHistorial_cuandoLaClienteLaCancelo() {
        comoLaura();
        when(reservaRepository.findDetalladasPorCliente(LAURA_ID)).thenReturn(List.of(
                reserva(1, LocalDateTime.of(2026, 11, 10, 9, 0), EstadoReserva.CANCELADA)));

        MisReservasResponse respuesta = servicioConReloj(JUEVES_05).consultarMisReservas();

        assertTrue(respuesta.getProximas().isEmpty());
        assertEquals("Cancelada", respuesta.getHistorial().get(0).getEstado());
        assertEquals("2026-11-10", respuesta.getHistorial().get(0).getFecha());
    }

    // HU-0019/CA-06
    @Test
    void debeInformarQueAunNoTieneReservas_cuandoElClienteNuncaHaReservado() {
        comoLaura();
        when(reservaRepository.findDetalladasPorCliente(LAURA_ID)).thenReturn(List.of());

        MisReservasResponse respuesta = servicioConReloj(JUEVES_05).consultarMisReservas();

        assertEquals(MSG_SIN_RESERVAS, respuesta.getMensaje());
        assertTrue(respuesta.getHistorial().isEmpty());
        assertTrue(respuesta.getProximas().isEmpty());
    }

    // HU-0019/CA-07
    @Test
    void debeConsultarSoloLasReservasDelClienteAutenticado_cuandoOtroClienteTieneReservas() {
        comoLaura();
        when(reservaRepository.findDetalladasPorCliente(LAURA_ID)).thenReturn(List.of());

        servicioConReloj(JUEVES_05).consultarMisReservas();

        verify(reservaRepository).findDetalladasPorCliente(LAURA_ID);
    }

    // HU-0019/CA-04
    @Test
    void debeMostrarElDetalleDeLaReservaConfirmada_cuandoEsDelCliente() {
        comoLaura();
        Reserva reserva = reserva(100, LocalDateTime.of(2026, 11, 9, 9, 0), EstadoReserva.CONFIRMADA);
        when(reservaRepository.findDetalladaPorIdYCliente(100, LAURA_ID)).thenReturn(Optional.of(reserva));

        ReservaResponse detalle = servicioConReloj(JUEVES_05).consultarDetalle(100);

        assertEquals("RSV-100", detalle.getCodigoReserva());
        assertEquals("Confirmada", detalle.getEstado());
        assertEquals("Barbería Centro", detalle.getNombreNegocio());
        assertEquals("Corte clásico", detalle.getNombreServicio());
        assertEquals("Carlos Ruiz", detalle.getNombreProveedor());
        assertEquals("2026-11-09", detalle.getFecha());
        assertEquals("09:00", detalle.getHoraInicio());
        assertEquals("09:30", detalle.getHoraFin());
        assertEquals(30, detalle.getDuracionMinutos());
        assertEquals(reserva.getFechaCreacion(), detalle.getFechaCreacion());
    }

    // HU-0019/CA-05
    @Test
    void debeMostrarEstadoFechaYMotivo_cuandoElDetalleEsDeUnaReservaCancelada() {
        comoLaura();
        Reserva reserva = reserva(100, LocalDateTime.of(2026, 11, 9, 9, 0), EstadoReserva.CANCELADA);
        OffsetDateTime cancelada = OffsetDateTime.of(2026, 11, 4, 18, 0, 0, 0, ZoneOffset.ofHours(-5));
        reserva.setCancelacion(CancelacionReserva.builder()
                .reservaId(100).canceladaPor(LAURA_ID).fechaCancelacion(cancelada).motivo("Viaje de trabajo").build());
        when(reservaRepository.findDetalladaPorIdYCliente(100, LAURA_ID)).thenReturn(Optional.of(reserva));

        ReservaResponse detalle = servicioConReloj(JUEVES_05).consultarDetalle(100);

        assertEquals("Cancelada", detalle.getEstado());
        assertEquals(cancelada, detalle.getFechaCancelacion());
        assertEquals("Viaje de trabajo", detalle.getMotivoCancelacion());
    }

    // HU-0019/CA-07
    @Test
    void debeResponderNoEncontrada_cuandoElDetallePedidoEsDeOtroCliente() {
        comoLaura();
        when(reservaRepository.findDetalladaPorIdYCliente(200, LAURA_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> servicioConReloj(JUEVES_05).consultarDetalle(200));

        assertEquals(MSG_RESERVA_NO_ENCONTRADA, ex.getMessage());
    }

    @Test
    void debeRechazarLaConsulta_cuandoElUsuarioNoEsCliente() {
        when(usuarioActual.esCliente()).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> servicioConReloj(JUEVES_05).consultarMisReservas());

        assertEquals(MSG_SOLO_CLIENTES, ex.getMessage());
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void debeRechazarElDetalle_cuandoElUsuarioNoEsCliente() {
        when(usuarioActual.esCliente()).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> servicioConReloj(JUEVES_05).consultarDetalle(100));

        assertEquals(MSG_SOLO_CLIENTES, ex.getMessage());
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void debeRechazarElDetalle_cuandoElIdEsNulo() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> servicioConReloj(JUEVES_05).consultarDetalle(null));

        assertEquals(MSG_ID_OBLIGATORIO, ex.getMessage());
        verifyNoInteractions(reservaRepository, usuarioActual);
    }

    private MisReservasService servicioConReloj(LocalDateTime ahora) {
        Clock reloj = Clock.fixed(ahora.atZone(ZONA).toInstant(), ZONA);
        return new MisReservasService(reservaRepository, new ClasificadorReservas(), new ReservaConsultaMapper(),
                usuarioActual, reloj);
    }

    private void comoLaura() {
        when(usuarioActual.esCliente()).thenReturn(true);
        when(usuarioActual.getId()).thenReturn(LAURA_ID);
    }

    private Reserva reserva(Integer id, LocalDateTime inicio, EstadoReserva estado) {
        Negocio negocio = Negocio.builder().idNegocio(5).propietarioId(7).nombreNegocio("Barbería Centro").build();
        UsuarioPlataforma carlos = UsuarioPlataforma.builder().idUsuario(12).nombre("Carlos").apellido("Ruiz").build();
        return Reserva.builder()
                .idReserva(id)
                .codigoReserva("RSV-" + id)
                .clienteId(LAURA_ID)
                .proveedorId(12)
                .servicioId(1)
                .inicio(inicio)
                .fin(inicio.plusMinutes(30))
                .nombreServicio("Corte clásico")
                .duracionMinutos(30)
                .estado(estado)
                .fechaCreacion(OffsetDateTime.of(2026, 11, 1, 8, 0, 0, 0, ZoneOffset.ofHours(-5)))
                .servicio(Servicio.builder().idServicio(1).nombre("Corte clásico").negocio(negocio).build())
                .perfilProveedor(PerfilProveedor.builder().usuarioId(12).usuario(carlos).build())
                .build();
    }
}
