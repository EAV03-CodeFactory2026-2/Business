package com.udea.business.services;

import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.entities.CancelacionReserva;
import com.udea.business.entities.DiaSemana;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.FranjaServicio;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.PerfilProveedor;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.UsuarioPlataforma;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.CancelacionReservaRepository;
import com.udea.business.repositories.FranjaServicioRepository;
import com.udea.business.repositories.PerfilProveedorRepository;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.repositories.ServicioRepository;
import com.udea.business.repositories.UsuarioPlataformaRepository;
import com.udea.business.security.IUsuarioActual;
import com.udea.business.services.acceso.IAccesoReservaPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.udea.business.services.ReservaService.MSG_FRANJA_RESERVADA;
import static com.udea.business.services.ReservaService.MSG_RESERVA_CANCELADA;
import static com.udea.business.services.ReservaService.MSG_RESERVA_CONFIRMADA;
import static com.udea.business.services.ReservaService.MSG_RESERVA_NO_ENCONTRADA;
import static com.udea.business.services.ReservaService.MSG_SERVICIO_NO_ENCONTRADO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final Instant AHORA = Instant.parse("2026-11-05T15:00:00Z");
    private static final Integer SERVICIO_ID = 1;
    private static final Integer PROVEEDOR_ID = 12;
    private static final Integer CLIENTE_ID = 20;
    private static final Integer FRANJA_ID = 10;
    private static final Integer NEGOCIO_ID = 5;
    private static final Integer RESERVA_ID = 100;
    private static final String CODIGO = "RSV-00000001";
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalDateTime INICIO = LocalDateTime.of(LUNES, NUEVE);
    private static final LocalDateTime FIN = INICIO.plusMinutes(30);

    @Mock
    private IReservaValidator validator;

    @Mock
    private ICancelacionReservaValidator cancelacionValidator;

    @Mock
    private ICodigoReservaGenerator codigoReservaGenerator;

    @Mock
    private IUsuarioActual usuarioActual;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private CancelacionReservaRepository cancelacionReservaRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private PerfilProveedorRepository perfilProveedorRepository;

    @Mock
    private UsuarioPlataformaRepository usuarioPlataformaRepository;

    @Mock
    private FranjaServicioRepository franjaServicioRepository;

    @Mock
    private IAccesoReservaPolicy accesoReservaPolicy;

    private ReservaService service;

    @BeforeEach
    void setUp() {
        service = new ReservaService(validator, cancelacionValidator, codigoReservaGenerator, usuarioActual,
                reservaRepository, cancelacionReservaRepository, servicioRepository, perfilProveedorRepository,
                usuarioPlataformaRepository, franjaServicioRepository, Clock.fixed(AHORA, ZONA), accesoReservaPolicy);
    }

    // HU-0018/CA-01
    @Test
    void debeCrearLaReservaConfirmadaConResumen_cuandoLaFranjaEstaDisponible() {
        CrearReservaRequest request = solicitud();
        request.setNotas("  Prefiero corte con tijera  ");
        Servicio servicio = servicio("COP");
        prepararCreacion(request, servicio);
        when(usuarioPlataformaRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(usuario(CLIENTE_ID, "Laura", "Pérez")));

        ReservaResponse respuesta = service.crearReserva(request);

        assertEquals(MSG_RESERVA_CONFIRMADA, respuesta.getMensaje());
        assertEquals(RESERVA_ID, respuesta.getIdReserva());
        assertEquals(CODIGO, respuesta.getCodigoReserva());
        assertEquals("Confirmada", respuesta.getEstado());
        assertEquals("Corte clásico", respuesta.getNombreServicio());
        assertEquals(NEGOCIO_ID, respuesta.getNegocioId());
        assertEquals("Barbería Centro", respuesta.getNombreNegocio());
        assertEquals("Carlos Ruiz", respuesta.getNombreProveedor());
        assertEquals("Laura Pérez", respuesta.getNombreCliente());
        assertEquals("2026-11-09", respuesta.getFecha());
        assertEquals("09:00", respuesta.getHoraInicio());
        assertEquals("09:30", respuesta.getHoraFin());
        assertEquals(30, respuesta.getDuracionMinutos());
        assertEquals(new BigDecimal("25000.00"), respuesta.getPrecio());
        assertEquals("COP", respuesta.getMoneda());
        assertEquals("Prefiero corte con tijera", respuesta.getNotas());
        assertEquals("\"Corte clásico\", \"Barbería Centro\", \"Carlos Ruiz\", lunes 09/11/2026, 09:00-09:30",
                respuesta.getResumen());
        assertEquals(OffsetDateTime.now(Clock.fixed(AHORA, ZONA)), respuesta.getFechaCreacion());

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).saveAndFlush(captor.capture());
        Reserva guardada = captor.getValue();
        assertEquals(EstadoReserva.CONFIRMADA, guardada.getEstado());
        assertEquals(CLIENTE_ID, guardada.getClienteId());
        assertEquals(PROVEEDOR_ID, guardada.getProveedorId());
        assertEquals(SERVICIO_ID, guardada.getServicioId());
        assertEquals(INICIO, guardada.getInicio());
        assertEquals(FIN, guardada.getFin());
        assertEquals("Corte clásico", guardada.getNombreServicio());
        assertEquals(30, guardada.getDuracionMinutos());
        assertEquals(new BigDecimal("25000.00"), guardada.getPrecioServicio());
    }

    @Test
    void debeUsarMonedaCopYDescartarNotasEnBlanco_cuandoElNegocioNoTieneMoneda() {
        CrearReservaRequest request = solicitud();
        request.setNotas("   ");
        prepararCreacion(request, servicio(null));
        when(usuarioPlataformaRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        ReservaResponse respuesta = service.crearReserva(request);

        assertEquals("COP", respuesta.getMoneda());
        assertNull(respuesta.getNotas());
        assertNull(respuesta.getNombreCliente());
    }

    @Test
    void debeConservarLaMonedaDelNegocio_cuandoEstaDefinida() {
        CrearReservaRequest request = solicitud();
        prepararCreacion(request, servicio("USD"));
        when(usuarioPlataformaRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        ReservaResponse respuesta = service.crearReserva(request);

        assertEquals("USD", respuesta.getMoneda());
        assertNull(respuesta.getNotas());
    }

    @Test
    void debeUsarMonedaCopYResumenSinNegocio_cuandoElServicioNoTieneNegocio() {
        CrearReservaRequest request = solicitud();
        Servicio servicio = servicio("COP");
        servicio.setNegocio(null);
        prepararCreacion(request, servicio);
        when(usuarioPlataformaRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        ReservaResponse respuesta = service.crearReserva(request);

        assertEquals("COP", respuesta.getMoneda());
        assertNull(respuesta.getNombreNegocio());
        assertNull(respuesta.getNegocioId());
        assertEquals("\"Corte clásico\", \"\", \"Carlos Ruiz\", lunes 09/11/2026, 09:00-09:30", respuesta.getResumen());
    }

    // HU-0018/CA-01
    @Test
    void debeTomarElServicioDeLaFranja_cuandoSoloSeEnviaLaFranja() {
        CrearReservaRequest request = CrearReservaRequest.builder().franjaId(FRANJA_ID).fecha(LUNES).build();
        Servicio servicio = servicio("COP");
        FranjaServicio franja = franja(servicio);
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.of(franja));
        when(usuarioActual.getId()).thenReturn(CLIENTE_ID);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));
        when(validator.obtenerYValidarFranja(request, servicio)).thenReturn(franja);
        when(codigoReservaGenerator.generarCodigo()).thenReturn(CODIGO);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(inv -> conId(inv.getArgument(0)));
        when(usuarioPlataformaRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        ReservaResponse respuesta = service.crearReserva(request);

        assertEquals(SERVICIO_ID, respuesta.getServicioId());
        assertEquals("09:00", respuesta.getHoraInicio());
    }

    // HU-0018/CA-09
    @Test
    void debeRechazarLaReserva_cuandoLaFranjaEnviadaYaNoExiste() {
        CrearReservaRequest request = CrearReservaRequest.builder().franjaId(FRANJA_ID).fecha(LUNES).build();
        when(usuarioActual.getId()).thenReturn(CLIENTE_ID);
        when(franjaServicioRepository.findById(FRANJA_ID)).thenReturn(Optional.empty());

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.crearReserva(request));

        assertEquals(ReservaValidator.MSG_FRANJA_NO_DISPONIBLE, ex.getMessage());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void debeResponderServicioNoEncontrado_cuandoElServicioNoExiste() {
        CrearReservaRequest request = solicitud();
        when(usuarioActual.getId()).thenReturn(CLIENTE_ID);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.crearReserva(request));

        assertEquals(MSG_SERVICIO_NO_ENCONTRADO, ex.getMessage());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    // HU-0018/CA-03
    @Test
    void debeBloquearAlProveedorAntesDeValidarCruces_cuandoSeCreaUnaReserva() {
        CrearReservaRequest request = solicitud();
        prepararCreacion(request, servicio("COP"));
        when(usuarioPlataformaRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        service.crearReserva(request);

        InOrder orden = inOrder(perfilProveedorRepository, validator, reservaRepository);
        orden.verify(perfilProveedorRepository).findByIdConBloqueo(PROVEEDOR_ID);
        orden.verify(validator).validarDisponibilidadProveedor(PROVEEDOR_ID, INICIO, FIN);
        orden.verify(validator).validarDisponibilidadCliente(CLIENTE_ID, INICIO, FIN);
        orden.verify(reservaRepository).saveAndFlush(any(Reserva.class));
    }

    // HU-0018/CA-03
    @Test
    void debeInformarQueLaFranjaFueReservada_cuandoOtraReservaSimultaneaGanaAlGuardar() {
        CrearReservaRequest request = solicitud();
        Servicio servicio = servicio("COP");
        when(usuarioActual.getId()).thenReturn(CLIENTE_ID);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));
        when(validator.obtenerYValidarFranja(request, servicio)).thenReturn(franja(servicio));
        when(codigoReservaGenerator.generarCodigo()).thenReturn(CODIGO);
        when(reservaRepository.saveAndFlush(any(Reserva.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> service.crearReserva(request));

        assertEquals(MSG_FRANJA_RESERVADA, ex.getMessage());
    }

    // HU-0018/CA-04
    @Test
    void debeRechazarLaReserva_cuandoLaFranjaFueTomadaMientrasElClienteDecidia() {
        CrearReservaRequest request = solicitud();
        Servicio servicio = servicio("COP");
        when(usuarioActual.getId()).thenReturn(CLIENTE_ID);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));
        when(validator.obtenerYValidarFranja(request, servicio)).thenReturn(franja(servicio));
        doThrow(new RecursoDuplicadoException(ReservaValidator.MSG_FRANJA_RESERVADA))
                .when(validator).validarDisponibilidadProveedor(PROVEEDOR_ID, INICIO, FIN);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> service.crearReserva(request));

        assertEquals(ReservaValidator.MSG_FRANJA_RESERVADA, ex.getMessage());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    // HU-0018/CA-05
    @Test
    void debeRechazarLaReserva_cuandoElClienteTieneOtraReservaQueSeCruza() {
        CrearReservaRequest request = solicitud();
        Servicio servicio = servicio("COP");
        when(usuarioActual.getId()).thenReturn(CLIENTE_ID);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));
        when(validator.obtenerYValidarFranja(request, servicio)).thenReturn(franja(servicio));
        doThrow(new SolicitudInvalidaException(ReservaValidator.MSG_CRUCE_CLIENTE))
                .when(validator).validarDisponibilidadCliente(CLIENTE_ID, INICIO, FIN);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.crearReserva(request));

        assertEquals(ReservaValidator.MSG_CRUCE_CLIENTE, ex.getMessage());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    // HU-0018/CA-06
    @Test
    void debeRechazarLaReserva_cuandoLasNotasSonDemasiadoLargas() {
        CrearReservaRequest request = solicitud();
        request.setNotas("a".repeat(251));
        doThrow(new SolicitudInvalidaException(ReservaValidator.MSG_NOTAS_LARGAS))
                .when(validator).validarNotas(request.getNotas());

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.crearReserva(request));

        assertEquals(ReservaValidator.MSG_NOTAS_LARGAS, ex.getMessage());
        verifyNoInteractions(servicioRepository, reservaRepository);
    }

    // HU-0018/CA-07
    @Test
    void debeRechazarLaReserva_cuandoQuienReservaNoEsCliente() {
        CrearReservaRequest request = solicitud();
        doThrow(new AccesoDenegadoException(ReservaValidator.MSG_SOLO_CLIENTES))
                .when(validator).validarRolCliente(usuarioActual);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> service.crearReserva(request));

        assertEquals(ReservaValidator.MSG_SOLO_CLIENTES, ex.getMessage());
        verifyNoInteractions(servicioRepository, reservaRepository, perfilProveedorRepository);
    }

    // HU-0018/CA-08
    @Test
    void debeRechazarLaReserva_cuandoElHorarioNoEsUnaFranjaDefinida() {
        CrearReservaRequest request = solicitud();
        request.setHoraInicio(LocalTime.of(8, 0));
        Servicio servicio = servicio("COP");
        when(usuarioActual.getId()).thenReturn(CLIENTE_ID);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));
        when(validator.obtenerYValidarFranja(request, servicio))
                .thenThrow(new SolicitudInvalidaException(ReservaValidator.MSG_HORARIO_NO_DISPONIBLE));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.crearReserva(request));

        assertEquals(ReservaValidator.MSG_HORARIO_NO_DISPONIBLE, ex.getMessage());
        verify(reservaRepository, never()).saveAndFlush(any());
    }

    @Test
    void debeDevolverElDetalle_cuandoLaReservaExisteYElUsuarioPuedeVerla() {
        Reserva reserva = reservaGuardada(EstadoReserva.CONFIRMADA);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(accesoReservaPolicy.puedeVer(reserva)).thenReturn(true);
        prepararDatosDeDetalle();

        ReservaResponse respuesta = service.consultarPorId(RESERVA_ID);

        assertEquals(CODIGO, respuesta.getCodigoReserva());
        assertEquals("Confirmada", respuesta.getEstado());
        assertEquals(MSG_RESERVA_CONFIRMADA, respuesta.getMensaje());
        assertEquals("Barbería Centro", respuesta.getNombreNegocio());
        assertEquals("Carlos Ruiz", respuesta.getNombreProveedor());
        assertEquals("Laura Pérez", respuesta.getNombreCliente());
        assertNull(respuesta.getFechaCancelacion());
        verifyNoInteractions(cancelacionReservaRepository);
    }

    @Test
    void debeResponderNoEncontrada_cuandoElUsuarioNoPuedeVerLaReserva() {
        Reserva reserva = reservaGuardada(EstadoReserva.CONFIRMADA);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(accesoReservaPolicy.puedeVer(reserva)).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.consultarPorId(RESERVA_ID));

        assertEquals(MSG_RESERVA_NO_ENCONTRADA, ex.getMessage());
    }

    @Test
    void debeResponderNoEncontrada_cuandoLaReservaNoExiste() {
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.consultarPorId(RESERVA_ID));

        assertEquals(MSG_RESERVA_NO_ENCONTRADA, ex.getMessage());
    }

    @Test
    void debeRechazarLaConsulta_cuandoElIdEsNulo() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarPorId(null));

        assertEquals("El identificador de la reserva es obligatorio.", ex.getMessage());
    }

    // HU-0019/CA-05
    @Test
    void debeIncluirFechaYMotivoDeCancelacion_cuandoLaReservaCanceladaLosTraeCargados() {
        Reserva reserva = reservaGuardada(EstadoReserva.CANCELADA);
        OffsetDateTime fechaCancelacion = OffsetDateTime.of(2026, 11, 8, 18, 0, 0, 0, ZoneOffset.ofHours(-5));
        reserva.setCancelacion(cancelacion(fechaCancelacion, "Viaje de trabajo"));
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(accesoReservaPolicy.puedeVer(reserva)).thenReturn(true);
        prepararDatosDeDetalle();

        ReservaResponse respuesta = service.consultarPorId(RESERVA_ID);

        assertEquals("Cancelada", respuesta.getEstado());
        assertEquals(MSG_RESERVA_CANCELADA, respuesta.getMensaje());
        assertEquals(fechaCancelacion, respuesta.getFechaCancelacion());
        assertEquals("Viaje de trabajo", respuesta.getMotivoCancelacion());
        verifyNoInteractions(cancelacionReservaRepository);
    }

    @Test
    void debeBuscarLaCancelacion_cuandoLaReservaCanceladaNoLaTraeCargada() {
        Reserva reserva = reservaGuardada(EstadoReserva.CANCELADA);
        OffsetDateTime fechaCancelacion = OffsetDateTime.of(2026, 11, 8, 18, 0, 0, 0, ZoneOffset.ofHours(-5));
        when(reservaRepository.findByCodigoReserva(CODIGO)).thenReturn(Optional.of(reserva));
        when(accesoReservaPolicy.puedeVer(reserva)).thenReturn(true);
        when(cancelacionReservaRepository.findByReservaId(RESERVA_ID))
                .thenReturn(Optional.of(cancelacion(fechaCancelacion, "Viaje de trabajo")));
        prepararDatosDeDetalle();

        ReservaResponse respuesta = service.consultarPorCodigo("  " + CODIGO + "  ");

        assertEquals(fechaCancelacion, respuesta.getFechaCancelacion());
        assertEquals("Viaje de trabajo", respuesta.getMotivo());
    }

    @Test
    void debeDevolverLaCanceladaSinDatosDeCancelacion_cuandoNoHayRegistroDeCancelacion() {
        Reserva reserva = reservaGuardada(EstadoReserva.CANCELADA);
        when(reservaRepository.findByCodigoReserva(CODIGO)).thenReturn(Optional.of(reserva));
        when(accesoReservaPolicy.puedeVer(reserva)).thenReturn(true);
        when(cancelacionReservaRepository.findByReservaId(RESERVA_ID)).thenReturn(Optional.empty());
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.empty());
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.empty());
        when(usuarioPlataformaRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        ReservaResponse respuesta = service.consultarPorCodigo(CODIGO);

        assertEquals("Cancelada", respuesta.getEstado());
        assertNull(respuesta.getFechaCancelacion());
        assertNull(respuesta.getNombreNegocio());
        assertNull(respuesta.getNombreProveedor());
        assertEquals("\"Corte clásico\", \"\", \"\", lunes 09/11/2026, 09:00-09:30", respuesta.getResumen());
    }

    @Test
    void debeMostrarEstadoConfirmada_cuandoLaReservaNoTieneEstado() {
        Reserva reserva = reservaGuardada(null);
        PerfilProveedor sinUsuario = PerfilProveedor.builder().usuarioId(PROVEEDOR_ID).build();
        when(reservaRepository.findByCodigoReserva(CODIGO)).thenReturn(Optional.of(reserva));
        when(accesoReservaPolicy.puedeVer(reserva)).thenReturn(true);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio("COP")));
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(sinUsuario));
        when(usuarioPlataformaRepository.findById(CLIENTE_ID)).thenReturn(Optional.empty());

        ReservaResponse respuesta = service.consultarPorCodigo(CODIGO);

        assertEquals("Confirmada", respuesta.getEstado());
        assertNull(respuesta.getNombreProveedor());
    }

    @Test
    void debeResponderNoEncontrada_cuandoElCodigoEsDeUnaReservaAjena() {
        Reserva reserva = reservaGuardada(EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByCodigoReserva(CODIGO)).thenReturn(Optional.of(reserva));
        when(accesoReservaPolicy.puedeVer(reserva)).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.consultarPorCodigo(CODIGO));

        assertEquals(MSG_RESERVA_NO_ENCONTRADA, ex.getMessage());
    }

    @Test
    void debeRechazarLaConsulta_cuandoElCodigoEsNulo() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarPorCodigo(null));

        assertEquals("El código de la reserva es obligatorio.", ex.getMessage());
    }

    @Test
    void debeRechazarLaConsulta_cuandoElCodigoEstaEnBlanco() {
        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarPorCodigo("   "));

        assertEquals("El código de la reserva es obligatorio.", ex.getMessage());
        verifyNoInteractions(reservaRepository);
    }

    private void prepararCreacion(CrearReservaRequest request, Servicio servicio) {
        when(usuarioActual.getId()).thenReturn(CLIENTE_ID);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));
        when(validator.obtenerYValidarFranja(request, servicio)).thenReturn(franja(servicio));
        when(codigoReservaGenerator.generarCodigo()).thenReturn(CODIGO);
        when(reservaRepository.saveAndFlush(any(Reserva.class))).thenAnswer(inv -> conId(inv.getArgument(0)));
    }

    private void prepararDatosDeDetalle() {
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio("COP")));
        when(perfilProveedorRepository.findById(PROVEEDOR_ID)).thenReturn(Optional.of(carlos()));
        when(usuarioPlataformaRepository.findById(CLIENTE_ID)).thenReturn(Optional.of(usuario(CLIENTE_ID, "Laura", "Pérez")));
    }

    private Reserva conId(Reserva reserva) {
        reserva.setIdReserva(RESERVA_ID);
        return reserva;
    }

    private CrearReservaRequest solicitud() {
        return CrearReservaRequest.builder()
                .servicioId(SERVICIO_ID)
                .proveedorId(PROVEEDOR_ID)
                .fecha(LUNES)
                .horaInicio(NUEVE)
                .build();
    }

    private Servicio servicio(String moneda) {
        return Servicio.builder()
                .idServicio(SERVICIO_ID)
                .nombre("Corte clásico")
                .duracionMinutos(30)
                .precio(new BigDecimal("25000.00"))
                .negocio(Negocio.builder()
                        .idNegocio(NEGOCIO_ID)
                        .propietarioId(7)
                        .nombreNegocio("Barbería Centro")
                        .monedaCodigoIso(moneda)
                        .build())
                .build();
    }

    private FranjaServicio franja(Servicio servicio) {
        return FranjaServicio.builder()
                .idFranja(FRANJA_ID)
                .servicio(servicio)
                .proveedor(carlos())
                .diaSemana(DiaSemana.LUNES)
                .horaInicio(NUEVE)
                .build();
    }

    private PerfilProveedor carlos() {
        return PerfilProveedor.builder()
                .usuarioId(PROVEEDOR_ID)
                .usuario(usuario(PROVEEDOR_ID, "Carlos", "Ruiz"))
                .build();
    }

    private UsuarioPlataforma usuario(Integer id, String nombre, String apellido) {
        return UsuarioPlataforma.builder().idUsuario(id).nombre(nombre).apellido(apellido).estado("Activo").build();
    }

    private Reserva reservaGuardada(EstadoReserva estado) {
        Reserva reserva = Reserva.builder()
                .idReserva(RESERVA_ID)
                .codigoReserva(CODIGO)
                .clienteId(CLIENTE_ID)
                .proveedorId(PROVEEDOR_ID)
                .servicioId(SERVICIO_ID)
                .inicio(INICIO)
                .fin(FIN)
                .nombreServicio("Corte clásico")
                .duracionMinutos(30)
                .precioServicio(new BigDecimal("25000.00"))
                .monedaCodigoIso("COP")
                .fechaCreacion(OffsetDateTime.now(Clock.fixed(AHORA, ZONA)))
                .build();
        reserva.setEstado(estado);
        return reserva;
    }

    private CancelacionReserva cancelacion(OffsetDateTime fecha, String motivo) {
        return CancelacionReserva.builder()
                .reservaId(RESERVA_ID)
                .canceladaPor(CLIENTE_ID)
                .fechaCancelacion(fecha)
                .motivo(motivo)
                .build();
    }
}
