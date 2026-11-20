package com.udea.business.services;

import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.entities.DiaSemana;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.FranjaServicio;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.PerfilProveedor;
import com.udea.business.entities.RecursoNegocio;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.ServicioRecurso;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.FranjaServicioRepository;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.repositories.ServicioRecursoRepository;
import com.udea.business.security.IUsuarioActual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.udea.business.services.ReservaValidator.MSG_CRUCE_CLIENTE;
import static com.udea.business.services.ReservaValidator.MSG_FECHA_OBLIGATORIA;
import static com.udea.business.services.ReservaValidator.MSG_FRANJA_NO_DISPONIBLE;
import static com.udea.business.services.ReservaValidator.MSG_FRANJA_RESERVADA;
import static com.udea.business.services.ReservaValidator.MSG_HORARIO_NO_DISPONIBLE;
import static com.udea.business.services.ReservaValidator.MSG_HORA_OBLIGATORIA;
import static com.udea.business.services.ReservaValidator.MSG_NOTAS_LARGAS;
import static com.udea.business.services.ReservaValidator.MSG_PROVEEDOR_OBLIGATORIO;
import static com.udea.business.services.ReservaValidator.MSG_SERVICIO_OBLIGATORIO;
import static com.udea.business.services.ReservaValidator.MSG_SOLICITUD_VACIA;
import static com.udea.business.services.ReservaValidator.MSG_SOLO_CLIENTES;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaValidatorTest {

    private static final Integer SERVICIO_ID = 1;
    private static final Integer OTRO_SERVICIO_ID = 2;
    private static final Integer PROVEEDOR_ID = 12;
    private static final Integer CLIENTE_ID = 20;
    private static final Integer FRANJA_ID = 10;
    private static final Integer NEGOCIO_ID = 5;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalDateTime INICIO = LocalDateTime.of(LUNES, NUEVE);
    private static final LocalDateTime FIN = INICIO.plusMinutes(30);

    @Mock
    private FranjaServicioRepository franjaServicioRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ServicioRecursoRepository servicioRecursoRepository;

    @Mock
    private IUsuarioActual usuarioActual;

    @InjectMocks
    private ReservaValidator validator;

    @Test
    void debePermitirReservar_cuandoElUsuarioEsCliente() {
        when(usuarioActual.esCliente()).thenReturn(true);

        assertDoesNotThrow(() -> validator.validarRolCliente(usuarioActual));
    }

    @Test
    void debeRechazarLaReserva_cuandoElUsuarioEsProveedor() {
        when(usuarioActual.esCliente()).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> validator.validarRolCliente(usuarioActual));

        assertEquals(MSG_SOLO_CLIENTES, ex.getMessage());
    }

    @Test
    void debeRechazarLaReserva_cuandoNoHayUsuarioAutenticado() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> validator.validarRolCliente(null));

        assertEquals(MSG_SOLO_CLIENTES, ex.getMessage());
    }

    @Test
    void debeRechazarLasNotas_cuandoSuperan250Caracteres() {
        String notas = "a".repeat(251);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarNotas(notas));

        assertEquals(MSG_NOTAS_LARGAS, ex.getMessage());
    }

    @Test
    void debeAceptarLasNotas_cuandoTienenExactamente250Caracteres() {
        String notas = "a".repeat(250);

        assertDoesNotThrow(() -> validator.validarNotas(notas));
    }

    @Test
    void debeAceptarLaReserva_cuandoNoTieneNotas() {
        assertDoesNotThrow(() -> validator.validarNotas(null));
    }

    @Test
    void debeRechazarLaSolicitud_cuandoEsNula() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarParametrosBasicos(null));

        assertEquals(MSG_SOLICITUD_VACIA, ex.getMessage());
    }

    @Test
    void debeRechazarLaSolicitud_cuandoNoTieneFecha() {
        CrearReservaRequest request = solicitudPorHorario();
        request.setFecha(null);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarParametrosBasicos(request));

        assertEquals(MSG_FECHA_OBLIGATORIA, ex.getMessage());
    }

    @Test
    void debeRechazarLaSolicitud_cuandoNoTieneFranjaNiServicio() {
        CrearReservaRequest request = solicitudPorHorario();
        request.setServicioId(null);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarParametrosBasicos(request));

        assertEquals(MSG_SERVICIO_OBLIGATORIO, ex.getMessage());
    }

    @Test
    void debeRechazarLaSolicitud_cuandoNoTieneFranjaNiProveedor() {
        CrearReservaRequest request = solicitudPorHorario();
        request.setProveedorId(null);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarParametrosBasicos(request));

        assertEquals(MSG_PROVEEDOR_OBLIGATORIO, ex.getMessage());
    }

    @Test
    void debeRechazarLaSolicitud_cuandoNoTieneFranjaNiHoraInicio() {
        CrearReservaRequest request = solicitudPorHorario();
        request.setHoraInicio(null);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarParametrosBasicos(request));

        assertEquals(MSG_HORA_OBLIGATORIA, ex.getMessage());
    }

    @Test
    void debeAceptarLaSolicitud_cuandoTraeServicioProveedorFechaYHora() {
        assertDoesNotThrow(() -> validator.validarParametrosBasicos(solicitudPorHorario()));
    }

    @Test
    void debeAceptarLaSolicitud_cuandoSoloTraeFranjaYFecha() {
        CrearReservaRequest request = CrearReservaRequest.builder()
                .franjaId(FRANJA_ID)
                .fecha(LUNES)
                .build();

        assertDoesNotThrow(() -> validator.validarParametrosBasicos(request));
    }

    // HU-0018/CA-01
    @Test
    void debeDevolverLaFranja_cuandoElHorarioCorrespondeAUnaFranjaDefinida() {
        FranjaServicio franja = franja(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES, NUEVE);
        when(franjaServicioRepository.findByServicioIdServicioAndProveedorUsuarioIdAndDiaSemanaAndHoraInicio(
                SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES, NUEVE)).thenReturn(Optional.of(franja));

        FranjaServicio resultado = validator.obtenerYValidarFranja(solicitudPorHorario(), servicio());

        assertSame(franja, resultado);
    }

    // HU-0018/CA-08
    @Test
    void debeRechazarLaReserva_cuandoElHorarioNoEsUnaFranjaDefinida() {
        CrearReservaRequest request = solicitudPorHorario();
        request.setHoraInicio(LocalTime.of(8, 0));
        when(franjaServicioRepository.findByServicioIdServicioAndProveedorUsuarioIdAndDiaSemanaAndHoraInicio(
                SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES, LocalTime.of(8, 0))).thenReturn(Optional.empty());

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.obtenerYValidarFranja(request, servicio()));

        assertEquals(MSG_HORARIO_NO_DISPONIBLE, ex.getMessage());
    }

    @Test
    void debeDevolverLaFranja_cuandoLaFranjaSeleccionadaCoincideConLaSolicitud() {
        FranjaServicio franja = franja(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES, NUEVE);
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));

        FranjaServicio resultado = validator.obtenerYValidarFranja(solicitudPorFranja(), servicio());

        assertSame(franja, resultado);
    }

    @Test
    void debeDevolverLaFranja_cuandoSoloSeIndicaFranjaYFecha() {
        FranjaServicio franja = franja(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES, NUEVE);
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));
        CrearReservaRequest request = CrearReservaRequest.builder()
                .franjaId(FRANJA_ID)
                .fecha(LUNES)
                .build();

        FranjaServicio resultado = validator.obtenerYValidarFranja(request, null);

        assertSame(franja, resultado);
    }

    // HU-0018/CA-09
    @Test
    void debeRechazarLaReserva_cuandoLaFranjaFueEliminada() {
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.empty());

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.obtenerYValidarFranja(solicitudPorFranja(), servicio()));

        assertEquals(MSG_FRANJA_NO_DISPONIBLE, ex.getMessage());
    }

    @Test
    void debeRechazarLaReserva_cuandoLaFranjaEsDeOtroServicio() {
        FranjaServicio franja = franja(OTRO_SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES, NUEVE);
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.obtenerYValidarFranja(solicitudPorFranja(), servicio()));

        assertEquals(MSG_FRANJA_NO_DISPONIBLE, ex.getMessage());
    }

    @Test
    void debeRechazarLaReserva_cuandoLaFranjaEsDeOtroProveedor() {
        FranjaServicio franja = franja(SERVICIO_ID, 99, DiaSemana.LUNES, NUEVE);
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.obtenerYValidarFranja(solicitudPorFranja(), servicio()));

        assertEquals(MSG_FRANJA_NO_DISPONIBLE, ex.getMessage());
    }

    @Test
    void debeRechazarLaReserva_cuandoLaHoraNoCoincideConLaFranja() {
        FranjaServicio franja = franja(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.LUNES, LocalTime.of(10, 30));
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.obtenerYValidarFranja(solicitudPorFranja(), servicio()));

        assertEquals(MSG_FRANJA_NO_DISPONIBLE, ex.getMessage());
    }

    @Test
    void debeRechazarLaReserva_cuandoLaFechaNoCaeEnElDiaDeLaFranja() {
        FranjaServicio franja = franja(SERVICIO_ID, PROVEEDOR_ID, DiaSemana.MARTES, NUEVE);
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.obtenerYValidarFranja(solicitudPorFranja(), servicio()));

        assertEquals(MSG_FRANJA_NO_DISPONIBLE, ex.getMessage());
    }

    @Test
    void debePermitirLaReserva_cuandoElProveedorEstaLibre() {
        when(reservaRepository.findOverlappingByProveedor(PROVEEDOR_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validarDisponibilidadProveedor(PROVEEDOR_ID, INICIO, FIN));
    }

    // HU-0018/CA-04
    @Test
    void debeRechazarLaReserva_cuandoOtroClienteYaTomoLaFranja() {
        when(reservaRepository.findOverlappingByProveedor(PROVEEDOR_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(reservaDeServicio(SERVICIO_ID)));

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> validator.validarDisponibilidadProveedor(PROVEEDOR_ID, INICIO, FIN));

        assertEquals(MSG_FRANJA_RESERVADA, ex.getMessage());
    }

    @Test
    void debePermitirLaReserva_cuandoElClienteNoTieneCruces() {
        when(reservaRepository.findOverlappingByCliente(CLIENTE_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validarDisponibilidadCliente(CLIENTE_ID, INICIO, FIN));
    }

    // HU-0018/CA-05
    @Test
    void debeRechazarLaReserva_cuandoElClienteYaTieneUnaReservaQueSeCruza() {
        LocalDateTime finUnaHora = INICIO.plusMinutes(60);
        when(reservaRepository.findOverlappingByCliente(CLIENTE_ID, INICIO, finUnaHora, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(reservaDeServicio(OTRO_SERVICIO_ID)));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarDisponibilidadCliente(CLIENTE_ID, INICIO, finUnaHora));

        assertEquals(MSG_CRUCE_CLIENTE, ex.getMessage());
    }

    @Test
    void debeOmitirLaValidacionDeRecursos_cuandoNoHayServicio() {
        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(null, INICIO, FIN));

        verifyNoInteractions(servicioRecursoRepository, reservaRepository);
    }

    @Test
    void debeOmitirLaValidacionDeRecursos_cuandoElServicioNoTieneId() {
        Servicio servicio = servicio();
        servicio.setIdServicio(null);

        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(servicio, INICIO, FIN));

        verifyNoInteractions(servicioRecursoRepository, reservaRepository);
    }

    @Test
    void debeOmitirLaValidacionDeRecursos_cuandoFaltaElIntervalo() {
        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(servicio(), null, FIN));
        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(servicio(), INICIO, null));

        verifyNoInteractions(servicioRecursoRepository, reservaRepository);
    }

    @Test
    void debePermitirLaReserva_cuandoElServicioNoRequiereRecursos() {
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID)).thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));

        verifyNoInteractions(reservaRepository);
    }

    @Test
    void debePermitirLaReserva_cuandoNoHayReservasSolapadasYAlcanzanLasUnidades() {
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID))
                .thenReturn(List.of(requiere(SERVICIO_ID, recurso(30, "Camilla", 2), 2)));
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));
    }

    @Test
    void debeRechazarLaReserva_cuandoElServicioRequiereMasUnidadesQueLasDelRecurso() {
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID))
                .thenReturn(List.of(requiere(SERVICIO_ID, recurso(30, "Camilla", 2), 3)));
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of());

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));

        assertEquals("No hay unidades suficientes del recurso Camilla para esa franja.", ex.getMessage());
    }

    @Test
    void debeRechazarLaReserva_cuandoLaAsociacionNoTieneRecursoNiCantidadesSinSolapadas() {
        ServicioRecurso sinRecurso = ServicioRecurso.builder().recursoNegocio(null).cantidadRequerida((short) 1).build();
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID)).thenReturn(List.of(sinRecurso));
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of());

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));

        assertEquals("No hay unidades suficientes del recurso requerido para esa franja.", ex.getMessage());
    }

    @Test
    void debePermitirLaReserva_cuandoLaCantidadRequeridaYLaCapacidadNoEstanDefinidas() {
        RecursoNegocio sinCantidad = RecursoNegocio.builder().idRecurso(30).nombre("Camilla").build();
        ServicioRecurso asociacion = ServicioRecurso.builder().recursoNegocio(sinCantidad).build();
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID)).thenReturn(List.of(asociacion));
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));
    }

    @Test
    void debeRechazarLaReserva_cuandoLasReservasSolapadasYaOcupanLasUnidadesDelRecurso() {
        RecursoNegocio camilla = recurso(30, "Camilla", 2);
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID))
                .thenReturn(List.of(requiere(SERVICIO_ID, camilla, 1)));
        when(servicioRecursoRepository.findByServicioIdWithRecurso(OTRO_SERVICIO_ID))
                .thenReturn(List.of(requiere(OTRO_SERVICIO_ID, camilla, 1)));
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(reservaDeServicio(OTRO_SERVICIO_ID), reservaDeServicio(OTRO_SERVICIO_ID)));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));

        assertEquals("No hay unidades suficientes del recurso Camilla para esa franja.", ex.getMessage());
        verify(servicioRecursoRepository, times(1)).findByServicioIdWithRecurso(OTRO_SERVICIO_ID);
    }

    @Test
    void debePermitirLaReserva_cuandoLasReservasSolapadasDejanUnidadesLibres() {
        RecursoNegocio camilla = recurso(30, "Camilla", 2);
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID))
                .thenReturn(List.of(requiere(SERVICIO_ID, camilla, 1)));
        when(servicioRecursoRepository.findByServicioIdWithRecurso(OTRO_SERVICIO_ID))
                .thenReturn(List.of(requiere(OTRO_SERVICIO_ID, camilla, 1)));
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(reservaDeServicio(OTRO_SERVICIO_ID)));

        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));
    }

    @Test
    void debePermitirLaReserva_cuandoLasReservasSolapadasUsanOtrosRecursos() {
        ServicioRecurso sinCantidad = ServicioRecurso.builder().recursoNegocio(recurso(30, "Camilla", 2)).build();
        ServicioRecurso sinRecurso = ServicioRecurso.builder().cantidadRequerida((short) 1).build();
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID))
                .thenReturn(List.of(requiere(SERVICIO_ID, recurso(30, "Camilla", 1), 1)));
        when(servicioRecursoRepository.findByServicioIdWithRecurso(OTRO_SERVICIO_ID))
                .thenReturn(List.of(requiere(OTRO_SERVICIO_ID, recurso(31, "Sala privada", 1), 1), sinCantidad, sinRecurso));
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(reservaDeServicio(OTRO_SERVICIO_ID)));

        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));
    }

    @Test
    void debeRechazarLaReserva_cuandoHaySolapadasYLaAsociacionNoTieneRecurso() {
        ServicioRecurso sinRecurso = ServicioRecurso.builder().cantidadRequerida((short) 1).build();
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID)).thenReturn(List.of(sinRecurso));
        when(servicioRecursoRepository.findByServicioIdWithRecurso(OTRO_SERVICIO_ID)).thenReturn(List.of());
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(reservaDeServicio(OTRO_SERVICIO_ID)));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));

        assertEquals("No hay unidades suficientes del recurso requerido para esa franja.", ex.getMessage());
    }

    @Test
    void debePermitirLaReserva_cuandoHaySolapadasYNoSeDefinieronCantidades() {
        RecursoNegocio sinCantidad = RecursoNegocio.builder().idRecurso(30).nombre("Camilla").build();
        ServicioRecurso asociacion = ServicioRecurso.builder().recursoNegocio(sinCantidad).build();
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID)).thenReturn(List.of(asociacion));
        when(servicioRecursoRepository.findByServicioIdWithRecurso(OTRO_SERVICIO_ID)).thenReturn(List.of());
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of(reservaDeServicio(OTRO_SERVICIO_ID)));

        assertDoesNotThrow(() -> validator.validarDisponibilidadRecursos(servicio(), INICIO, FIN));
    }

    @Test
    void debeIndicarQueHayRecursos_cuandoLaValidacionPasa() {
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID)).thenReturn(List.of());

        assertTrue(validator.hayRecursosDisponibles(servicio(), INICIO, FIN));
    }

    @Test
    void debeIndicarQueNoHayRecursos_cuandoFaltanUnidades() {
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID))
                .thenReturn(List.of(requiere(SERVICIO_ID, recurso(30, "Camilla", 1), 2)));
        when(reservaRepository.findOverlappingConfirmedByNegocio(NEGOCIO_ID, INICIO, FIN, EstadoReserva.CONFIRMADA))
                .thenReturn(List.of());

        assertFalse(validator.hayRecursosDisponibles(servicio(), INICIO, FIN));
    }

    private CrearReservaRequest solicitudPorHorario() {
        return CrearReservaRequest.builder()
                .servicioId(SERVICIO_ID)
                .proveedorId(PROVEEDOR_ID)
                .fecha(LUNES)
                .horaInicio(NUEVE)
                .build();
    }

    private CrearReservaRequest solicitudPorFranja() {
        return CrearReservaRequest.builder()
                .franjaId(FRANJA_ID)
                .servicioId(SERVICIO_ID)
                .proveedorId(PROVEEDOR_ID)
                .fecha(LUNES)
                .horaInicio(NUEVE)
                .build();
    }

    private Servicio servicio() {
        return Servicio.builder()
                .idServicio(SERVICIO_ID)
                .nombre("Corte clásico")
                .duracionMinutos(30)
                .negocio(Negocio.builder().idNegocio(NEGOCIO_ID).nombreNegocio("Barbería Centro").build())
                .build();
    }

    private FranjaServicio franja(Integer servicioId, Integer proveedorId, DiaSemana dia, LocalTime hora) {
        return FranjaServicio.builder()
                .idFranja(FRANJA_ID)
                .servicio(Servicio.builder().idServicio(servicioId).duracionMinutos(30).build())
                .proveedor(PerfilProveedor.builder().usuarioId(proveedorId).build())
                .diaSemana(dia)
                .horaInicio(hora)
                .build();
    }

    private RecursoNegocio recurso(Integer id, String nombre, int cantidad) {
        return RecursoNegocio.builder()
                .idRecurso(id)
                .negocioId(NEGOCIO_ID)
                .nombre(nombre)
                .cantidad(cantidad)
                .estado("Disponible")
                .build();
    }

    private ServicioRecurso requiere(Integer servicioId, RecursoNegocio recurso, int unidades) {
        return ServicioRecurso.builder()
                .id(new ServicioRecurso.ServicioRecursoId(servicioId, recurso.getIdRecurso()))
                .recursoNegocio(recurso)
                .negocioId(NEGOCIO_ID)
                .cantidadRequerida((short) unidades)
                .build();
    }

    private Reserva reservaDeServicio(Integer servicioId) {
        return Reserva.builder()
                .servicioId(servicioId)
                .proveedorId(PROVEEDOR_ID)
                .inicio(INICIO)
                .fin(FIN)
                .estado(EstadoReserva.CONFIRMADA)
                .build();
    }
}
