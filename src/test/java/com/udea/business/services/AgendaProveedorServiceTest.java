package com.udea.business.services;

import com.udea.business.controllers.DTOs.AgendaProveedorResponse;
import com.udea.business.controllers.DTOs.ReservaAgendaResponse;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.UsuarioPlataforma;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.security.IUsuarioActual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static com.udea.business.services.AgendaProveedorService.MSG_SIN_RESERVAS;
import static com.udea.business.services.AgendaProveedorService.MSG_SOLO_PROVEEDORES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendaProveedorServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Integer CARLOS_ID = 12;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    private static final LocalDate MARTES = LocalDate.of(2026, 11, 10);
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 11, 11);

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private IUsuarioActual usuarioActual;

    private AgendaProveedorService service;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(LocalDateTime.of(LUNES, LocalTime.of(8, 0)).atZone(ZONA).toInstant(), ZONA);
        service = new AgendaProveedorService(reservaRepository, new ReservaConsultaMapper(), usuarioActual, reloj);
    }

    // HU-0022/CA-01
    @Test
    void debeMostrarLasDosReservasEnOrden_cuandoCarlosAtiendeEnVariosNegocios() {
        comoCarlos();
        Reserva laura = reserva(1, LUNES, LocalTime.of(9, 0), "Barbería Centro", "Laura", "Pérez", null, null);
        Reserva mario = reserva(2, LUNES, LocalTime.of(10, 0), "Barbería Norte", "Mario", "Gil", null, null);
        agendaDel(LUNES, List.of(laura, mario));

        AgendaProveedorResponse agenda = service.consultarMiAgenda(LUNES);

        assertEquals(2, agenda.getTotalReservas());
        ReservaAgendaResponse primera = agenda.getReservas().get(0);
        ReservaAgendaResponse segunda = agenda.getReservas().get(1);
        assertEquals("Laura Pérez", primera.getNombreCliente());
        assertEquals("09:00", primera.getHoraInicio());
        assertEquals("Barbería Centro", primera.getNombreNegocio());
        assertEquals("Mario Gil", segunda.getNombreCliente());
        assertEquals("10:00", segunda.getHoraInicio());
        assertEquals("Barbería Norte", segunda.getNombreNegocio());
        assertNull(agenda.getMensaje());
    }

    // HU-0022/CA-02
    @Test
    void debeMostrarTelefonoYNotasDelCliente_cuandoLauraDejoIndicaciones() {
        comoCarlos();
        Reserva laura = reserva(1, LUNES, LocalTime.of(9, 0), "Barbería Centro", "Laura", "Pérez",
                "+573001234567", "Prefiero corte con tijera");
        agendaDel(LUNES, List.of(laura));

        AgendaProveedorResponse agenda = service.consultarMiAgenda(LUNES);

        ReservaAgendaResponse reserva = agenda.getReservas().get(0);
        assertEquals("+573001234567", reserva.getTelefonoCliente());
        assertEquals("Prefiero corte con tijera", reserva.getNotas());
        assertEquals("Corte clásico", reserva.getNombreServicio());
        assertEquals("09:30", reserva.getHoraFin());
    }

    // HU-0022/CA-03
    @Test
    void debeMostrarLaAgendaDeHoy_cuandoNoSeIndicaFecha() {
        comoCarlos();
        agendaDel(LUNES, List.of(reserva(1, LUNES, LocalTime.of(9, 0), "Barbería Centro", "Laura", "Pérez", null, null)));

        AgendaProveedorResponse agenda = service.consultarMiAgenda(null);

        assertEquals("2026-11-09", agenda.getFecha());
        verify(reservaRepository).findAgendaProveedor(CARLOS_ID, LUNES.atStartOfDay(),
                MARTES.atStartOfDay(), EstadoReserva.CONFIRMADA);
    }

    // HU-0022/CA-04
    @Test
    void debeMostrarLaReservaDeLas8_cuandoConsultaLaAgendaDelMartes() {
        comoCarlos();
        agendaDel(MARTES, List.of(reserva(3, MARTES, LocalTime.of(8, 0), "Barbería Centro", "Laura", "Pérez", null, null)));

        AgendaProveedorResponse agenda = service.consultarMiAgenda(MARTES);

        assertEquals("2026-11-10", agenda.getFecha());
        assertEquals(1, agenda.getTotalReservas());
        assertEquals("08:00", agenda.getReservas().get(0).getHoraInicio());
    }

    // HU-0022/CA-05
    @Test
    void debeInformarQueNoTieneReservas_cuandoElDiaEstaVacio() {
        comoCarlos();
        agendaDel(MIERCOLES, List.of());

        AgendaProveedorResponse agenda = service.consultarMiAgenda(MIERCOLES);

        assertEquals(MSG_SIN_RESERVAS, agenda.getMensaje());
        assertEquals(0, agenda.getTotalReservas());
        assertTrue(agenda.getReservas().isEmpty());
    }

    // HU-0022/CA-06
    @Test
    void debeConsultarSoloReservasConfirmadas_cuandoArmaLaAgenda() {
        comoCarlos();
        agendaDel(LUNES, List.of());

        service.consultarMiAgenda(LUNES);

        verify(reservaRepository, never()).findAgendaProveedor(any(), any(), any(), eq(EstadoReserva.CANCELADA));
    }

    // HU-0022/CA-07
    @Test
    void debeConsultarConElIdDelProveedorAutenticado_cuandoOtroProveedorTieneReservas() {
        comoCarlos();
        agendaDel(LUNES, List.of());

        service.consultarMiAgenda(LUNES);

        verify(reservaRepository).findAgendaProveedor(eq(CARLOS_ID), any(), any(), eq(EstadoReserva.CONFIRMADA));
    }

    @Test
    void debeRechazarLaConsulta_cuandoElUsuarioNoEsProveedor() {
        when(usuarioActual.esProveedor()).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> service.consultarMiAgenda(LUNES));

        assertEquals(MSG_SOLO_PROVEEDORES, ex.getMessage());
        verifyNoInteractions(reservaRepository);
    }

    private void comoCarlos() {
        when(usuarioActual.esProveedor()).thenReturn(true);
        when(usuarioActual.getId()).thenReturn(CARLOS_ID);
    }

    private void agendaDel(LocalDate dia, List<Reserva> reservas) {
        when(reservaRepository.findAgendaProveedor(CARLOS_ID, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay(),
                EstadoReserva.CONFIRMADA)).thenReturn(reservas);
    }

    private Reserva reserva(Integer id, LocalDate dia, LocalTime hora, String negocio, String nombre,
                            String apellido, String telefono, String notas) {
        LocalDateTime inicio = LocalDateTime.of(dia, hora);
        return Reserva.builder()
                .idReserva(id)
                .codigoReserva("RSV-" + id)
                .proveedorId(CARLOS_ID)
                .inicio(inicio)
                .fin(inicio.plusMinutes(30))
                .nombreServicio("Corte clásico")
                .notas(notas)
                .estado(EstadoReserva.CONFIRMADA)
                .servicio(Servicio.builder()
                        .idServicio(1)
                        .negocio(Negocio.builder().idNegocio(id).nombreNegocio(negocio).build())
                        .build())
                .cliente(UsuarioPlataforma.builder()
                        .idUsuario(20 + id).nombre(nombre).apellido(apellido).telefono(telefono).build())
                .build();
    }
}
