package com.udea.business.services;

import com.udea.business.controllers.DTOs.FranjaDisponibleResponse;
import com.udea.business.entities.DiaSemana;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.EstadoServicio;
import com.udea.business.entities.FranjaServicio;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.PerfilProveedor;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.UsuarioPlataforma;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.FranjaServicioRepository;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.repositories.ServicioProveedorRepository;
import com.udea.business.repositories.ServicioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.udea.business.services.DisponibilidadService.MSG_FECHA_OBLIGATORIA;
import static com.udea.business.services.DisponibilidadService.MSG_SERVICIO_NO_ENCONTRADO;
import static com.udea.business.services.DisponibilidadService.MSG_SERVICIO_OBLIGATORIO;
import static com.udea.business.services.DisponibilidadService.MSG_SERVICIO_SIN_DISPONIBILIDAD;
import static com.udea.business.services.DisponibilidadService.MSG_SIN_FRANJAS_DISPONIBLES;
import static com.udea.business.services.DisponibilidadService.MSG_VENTANA_FECHA_INVALIDA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class DisponibilidadServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Integer SERVICIO_ID = 1;
    private static final Integer CARLOS_ID = 12;
    private static final Integer DIEGO_ID = 15;
    private static final LocalDate JUEVES = LocalDate.of(2026, 11, 5);
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 11, 15);

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private ServicioProveedorRepository servicioProveedorRepository;

    @Mock
    private FranjaServicioRepository franjaServicioRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private IReservaValidator validator;

    private DisponibilidadService service;

    @BeforeEach
    void setUp() {
        service = servicioConReloj(LocalDateTime.of(JUEVES, LocalTime.of(8, 0)));
    }

    // HU-0016/CA-01
    @Test
    void debeOfrecerOchoFranjasDeCarlos_cuandoNoTieneReservasElLunes() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 8));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals(8, franjas.size());
        assertEquals("08:00", franjas.get(0).getHoraInicio());
        assertEquals("08:30", franjas.get(0).getHoraFin());
        assertEquals("11:30", franjas.get(7).getHoraInicio());
        assertEquals("12:00", franjas.get(7).getHoraFin());
        assertTrue(franjas.stream().allMatch(f -> "Carlos Ruiz".equals(f.getNombreProveedor())));
        FranjaDisponibleResponse primera = franjas.get(0);
        assertEquals(SERVICIO_ID, primera.getServicioId());
        assertEquals("Corte clásico", primera.getNombreServicio());
        assertEquals(CARLOS_ID, primera.getProveedorId());
        assertEquals("2026-11-09", primera.getFecha());
        assertEquals("LUNES", primera.getDiaSemana());
        assertEquals(30, primera.getDuracionMinutos());
        assertEquals(new BigDecimal("25000.00"), primera.getPrecio());
        assertEquals("COP", primera.getMoneda());
    }

    // HU-0016/CA-02
    @Test
    void debeOfrecerSoloLasFranjasDefinidasDe45Minutos_cuandoElServicioEsBarbaYCorte() {
        Servicio barba = servicio(45);
        prepararServicio(barba, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(barba, carlos(), LocalTime.of(8, 0), 5));
        when(validator.hayRecursosDisponibles(eq(barba), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals(List.of("08:00-08:45", "08:45-09:30", "09:30-10:15", "10:15-11:00", "11:00-11:45"),
                franjas.stream().map(f -> f.getHoraInicio() + "-" + f.getHoraFin()).toList());
        assertFalse(franjas.stream().anyMatch(f -> "11:45".equals(f.getHoraInicio())));
    }

    // HU-0016/CA-03
    @Test
    void debeOcultarLaFranjaDeLas9_cuandoCarlosYaTieneUnaReservaConfirmada() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 8));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);
        reservaConfirmadaDeCarlosA(LocalTime.of(9, 0));

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals(7, franjas.size());
        assertFalse(franjas.stream().anyMatch(f -> "09:00".equals(f.getHoraInicio())));
    }

    // HU-0016/CA-04
    @Test
    void debeOcultarLaFranja_cuandoCarlosTieneUnaReservaEnOtroNegocio() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 8));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);
        reservaConfirmadaDeCarlosA(LocalTime.of(10, 0));

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertFalse(franjas.stream().anyMatch(f -> "10:00".equals(f.getHoraInicio())
                && CARLOS_ID.equals(f.getProveedorId())));
        verify(reservaRepository).findOverlappingByProveedor(CARLOS_ID,
                LocalDateTime.of(LUNES, LocalTime.of(10, 0)), LocalDateTime.of(LUNES, LocalTime.of(10, 30)),
                EstadoReserva.CONFIRMADA);
    }

    // HU-0016/CA-05
    @Test
    void debeOfrecerLaFranjaDeLas9_cuandoLaReservaDeEsaHoraFueCancelada() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 8));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertTrue(franjas.stream().anyMatch(f -> "09:00".equals(f.getHoraInicio())
                && "Carlos Ruiz".equals(f.getNombreProveedor())));
        verify(reservaRepository, never()).findOverlappingByProveedor(any(), any(), any(), eq(EstadoReserva.CANCELADA));
    }

    // HU-0016/CA-06
    @Test
    void debeOrdenarPorHoraYLuegoPorProveedor_cuandoElServicioTieneVariosProveedores() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 2);
        List<FranjaServicio> definidas = new ArrayList<>();
        definidas.add(franja(corte, diego(), LocalTime.of(8, 0)));
        definidas.addAll(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 2));
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(List.of(definidas.get(2), definidas.get(0), definidas.get(1)));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals(List.of("08:00-08:30 Carlos Ruiz", "08:00-08:30 Diego Salas", "08:30-09:00 Carlos Ruiz"),
                franjas.stream().map(f -> f.getHoraInicio() + "-" + f.getHoraFin() + " " + f.getNombreProveedor()).toList());
    }

    // HU-0016/CA-07
    @Test
    void debeEmpezarEnLaFranjaDe1030_cuandoSonLas1010DelMismoLunes() {
        service = servicioConReloj(LocalDateTime.of(LUNES, LocalTime.of(10, 10)));
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 8));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals("10:30", franjas.get(0).getHoraInicio());
        assertEquals("11:00", franjas.get(0).getHoraFin());
        assertEquals(3, franjas.size());
    }

    @Test
    void debeOcultarLaFranjaQueEmpiezaJustoAhora_cuandoLaHoraActualCoincideConElInicio() {
        service = servicioConReloj(LocalDateTime.of(LUNES, LocalTime.of(11, 0)));
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(11, 0), 2));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals(List.of("11:30"), franjas.stream().map(FranjaDisponibleResponse::getHoraInicio).toList());
    }

    // HU-0016/CA-08
    @Test
    void debeInformarQueNoHayFranjas_cuandoNingunProveedorAtiendeElDomingo() {
        prepararServicio(servicio(30), 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.DOMINGO))
                .thenReturn(List.of());

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarFranjasDisponibles(SERVICIO_ID, DOMINGO, null));

        assertEquals(MSG_SIN_FRANJAS_DISPONIBLES, ex.getMessage());
    }

    @Test
    void debeInformarQueNoHayFranjas_cuandoTodasLasFranjasDelDiaYaPasaron() {
        service = servicioConReloj(LocalDateTime.of(LUNES, LocalTime.of(13, 0)));
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 8));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null));

        assertEquals(MSG_SIN_FRANJAS_DISPONIBLES, ex.getMessage());
        verifyNoInteractions(reservaRepository);
    }

    // HU-0016/CA-09
    @Test
    void debeRechazarLaConsulta_cuandoLaFechaSuperaLos30DiasDesdeHoy() {
        prepararServicio(servicio(30), 1);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarFranjasDisponibles(SERVICIO_ID, LocalDate.of(2026, 12, 7), null));

        assertEquals(MSG_VENTANA_FECHA_INVALIDA, ex.getMessage());
        verifyNoInteractions(franjaServicioRepository);
    }

    @Test
    void debeRechazarLaConsulta_cuandoLaFechaEsElDia31() {
        prepararServicio(servicio(30), 1);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarFranjasDisponibles(SERVICIO_ID, LocalDate.of(2026, 12, 6), null));

        assertEquals(MSG_VENTANA_FECHA_INVALIDA, ex.getMessage());
    }

    @Test
    void debePermitirLaConsulta_cuandoLaFechaEsExactamenteElDia30() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        LocalDate dia30 = LocalDate.of(2026, 12, 5);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.SABADO))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 1));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, dia30, null);

        assertEquals(1, franjas.size());
        assertEquals("2026-12-05", franjas.get(0).getFecha());
    }

    @Test
    void debeRechazarLaConsulta_cuandoLaFechaYaPaso() {
        prepararServicio(servicio(30), 1);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarFranjasDisponibles(SERVICIO_ID, LocalDate.of(2026, 11, 4), null));

        assertEquals(MSG_VENTANA_FECHA_INVALIDA, ex.getMessage());
    }

    // HU-0016/CA-10
    @Test
    void debeInformarQueNoHayDisponibilidad_cuandoElServicioEstaNoAsignado() {
        Servicio masaje = servicio(60);
        masaje.setEstado(EstadoServicio.NO_ASIGNADO);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(masaje));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null));

        assertEquals(MSG_SERVICIO_SIN_DISPONIBILIDAD, ex.getMessage());
        verifyNoInteractions(franjaServicioRepository, servicioProveedorRepository);
    }

    // HU-0016/CA-10
    @Test
    void debeInformarQueNoHayDisponibilidad_cuandoElServicioNoTieneProveedoresVinculados() {
        prepararServicio(servicio(60), 0);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null));

        assertEquals(MSG_SERVICIO_SIN_DISPONIBILIDAD, ex.getMessage());
        verifyNoInteractions(franjaServicioRepository);
    }

    @Test
    void debeRechazarLaConsulta_cuandoNoSeIndicaElServicio() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarFranjasDisponibles(null, LUNES, null));

        assertEquals(MSG_SERVICIO_OBLIGATORIO, ex.getMessage());
        verifyNoInteractions(servicioRepository);
    }

    @Test
    void debeResponderServicioNoEncontrado_cuandoElServicioNoExiste() {
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null));

        assertEquals(MSG_SERVICIO_NO_ENCONTRADO, ex.getMessage());
    }

    @Test
    void debeRechazarLaConsulta_cuandoNoSeIndicaLaFecha() {
        prepararServicio(servicio(30), 1);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarFranjasDisponibles(SERVICIO_ID, null, null));

        assertEquals(MSG_FECHA_OBLIGATORIA, ex.getMessage());
    }

    @Test
    void debeConsultarSoloLasFranjasDelProveedor_cuandoSeFiltraPorProveedor() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 2);
        when(franjaServicioRepository.findByServicioIdServicioAndProveedorUsuarioIdAndDiaSemana(
                SERVICIO_ID, DIEGO_ID, DiaSemana.LUNES))
                .thenReturn(List.of(franja(corte, diego(), LocalTime.of(8, 0))));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, DIEGO_ID);

        assertEquals(1, franjas.size());
        assertEquals("Diego Salas", franjas.get(0).getNombreProveedor());
        verify(franjaServicioRepository, never()).findByServicioIdServicioAndDiaSemana(any(), any());
    }

    @Test
    void debeOcultarLaFranja_cuandoNoHayRecursosSuficientesEnEseHorario() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 2));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenAnswer(inv ->
                !LocalDateTime.of(LUNES, LocalTime.of(8, 0)).equals(inv.getArgument(1)));

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals(List.of("08:30"), franjas.stream().map(FranjaDisponibleResponse::getHoraInicio).toList());
    }

    @Test
    void debeCalcularElFinConLaDuracionDelServicio_cuandoLaFranjaNoTraeElServicio() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 1);
        FranjaServicio sinServicio = FranjaServicio.builder()
                .idFranja(50)
                .proveedor(null)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(LocalTime.of(9, 0))
                .build();
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(List.of(sinServicio));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals("09:30", franjas.get(0).getHoraFin());
        assertNull(franjas.get(0).getProveedorId());
        verifyNoInteractions(reservaRepository);
    }

    @Test
    void debeOrdenarAlFinalLaFranjaSinNombreDeProveedor_cuandoCoincideLaHora() {
        Servicio corte = servicio(30);
        prepararServicio(corte, 2);
        PerfilProveedor sinUsuario = PerfilProveedor.builder().usuarioId(30).build();
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(List.of(franja(corte, sinUsuario, LocalTime.of(8, 0)), franja(corte, carlos(), LocalTime.of(8, 0))));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals("Carlos Ruiz", franjas.get(0).getNombreProveedor());
        assertNull(franjas.get(1).getNombreProveedor());
    }

    @Test
    void debeUsarLaMonedaDelNegocio_cuandoEstaDefinida() {
        Servicio corte = servicio(30);
        corte.setNegocio(Negocio.builder().idNegocio(5).nombreNegocio("Spa Armonía").monedaCodigoIso("USD").build());
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 1));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals("USD", franjas.get(0).getMoneda());
    }

    @Test
    void debeUsarMonedaCop_cuandoElServicioNoTieneNegocio() {
        Servicio corte = servicio(30);
        corte.setNegocio(null);
        prepararServicio(corte, 1);
        when(franjaServicioRepository.findByServicioIdServicioAndDiaSemana(SERVICIO_ID, DiaSemana.LUNES))
                .thenReturn(franjasConsecutivas(corte, carlos(), LocalTime.of(8, 0), 1));
        when(validator.hayRecursosDisponibles(eq(corte), any(), any())).thenReturn(true);

        List<FranjaDisponibleResponse> franjas = service.consultarFranjasDisponibles(SERVICIO_ID, LUNES, null);

        assertEquals("COP", franjas.get(0).getMoneda());
    }

    private DisponibilidadService servicioConReloj(LocalDateTime ahora) {
        Clock reloj = Clock.fixed(ahora.atZone(ZONA).toInstant(), ZONA);
        return new DisponibilidadService(servicioRepository, servicioProveedorRepository,
                franjaServicioRepository, reservaRepository, validator, reloj);
    }

    private void prepararServicio(Servicio servicio, long proveedores) {
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));
        when(servicioProveedorRepository.countByServicioIdServicio(SERVICIO_ID)).thenReturn(proveedores);
    }

    private void reservaConfirmadaDeCarlosA(LocalTime hora) {
        LocalDateTime inicio = LocalDateTime.of(LUNES, hora);
        Reserva reserva = Reserva.builder()
                .proveedorId(CARLOS_ID)
                .inicio(inicio)
                .fin(inicio.plusMinutes(30))
                .estado(EstadoReserva.CONFIRMADA)
                .build();
        when(reservaRepository.findOverlappingByProveedor(eq(CARLOS_ID), any(), any(), eq(EstadoReserva.CONFIRMADA)))
                .thenAnswer(inv -> inicio.equals(inv.getArgument(1)) ? List.of(reserva) : List.of());
    }

    private Servicio servicio(int duracion) {
        return Servicio.builder()
                .idServicio(SERVICIO_ID)
                .nombre(duracion == 45 ? "Barba y corte" : "Corte clásico")
                .duracionMinutos(duracion)
                .precio(new BigDecimal("25000.00"))
                .estado(EstadoServicio.ASIGNADO)
                .negocio(Negocio.builder().idNegocio(5).nombreNegocio("Barbería Centro").build())
                .build();
    }

    private List<FranjaServicio> franjasConsecutivas(Servicio servicio, PerfilProveedor proveedor,
                                                     LocalTime desde, int cantidad) {
        List<FranjaServicio> franjas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            franjas.add(franja(servicio, proveedor, desde.plusMinutes((long) i * servicio.getDuracionMinutos())));
        }
        return franjas;
    }

    private FranjaServicio franja(Servicio servicio, PerfilProveedor proveedor, LocalTime hora) {
        return FranjaServicio.builder()
                .idFranja(hora.getHour() * 100 + hora.getMinute())
                .servicio(servicio)
                .proveedor(proveedor)
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(hora)
                .build();
    }

    private PerfilProveedor carlos() {
        return proveedor(CARLOS_ID, "Carlos", "Ruiz");
    }

    private PerfilProveedor diego() {
        return proveedor(DIEGO_ID, "Diego", "Salas");
    }

    private PerfilProveedor proveedor(Integer id, String nombre, String apellido) {
        return PerfilProveedor.builder()
                .usuarioId(id)
                .usuario(UsuarioPlataforma.builder().idUsuario(id).nombre(nombre).apellido(apellido).build())
                .build();
    }
}
