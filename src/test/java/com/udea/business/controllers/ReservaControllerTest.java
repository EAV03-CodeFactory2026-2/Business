package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.CancelarReservaRequest;
import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.controllers.DTOs.FranjaDisponibleResponse;
import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.services.CancelacionReservaValidator;
import com.udea.business.services.DisponibilidadService;
import com.udea.business.services.IDisponibilidadService;
import com.udea.business.services.IReservaService;
import com.udea.business.services.ReservaService;
import com.udea.business.services.ReservaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    private static final String RESERVAS = "/api/v1/reservas";
    private static final String CANCELAR = "/api/v1/reservas/{idReserva}/cancelar";
    private static final String FRANJAS = "/api/v1/servicios/{servicioId}/franjas-disponibles";
    private static final String CUERPO_RESERVA =
            "{\"servicioId\":1,\"proveedorId\":12,\"fecha\":\"2026-11-09\",\"horaInicio\":\"09:00\",\"notas\":\"Corte bajo\"}";
    private static final String CUERPO_CANCELAR = "{\"motivo\":\"Viaje de trabajo\",\"confirmacion\":true}";
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);

    @Mock
    private IReservaService reservaService;

    @Mock
    private IDisponibilidadService disponibilidadService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ReservaController(reservaService, disponibilidadService))
                .setControllerAdvice(new ProveedoresExceptionHandler())
                .build();
    }

    // HU-0018/CA-01
    @Test
    void debeResponder201ConElResumen_cuandoLaReservaSeCrea() throws Exception {
        when(reservaService.crearReserva(any(CrearReservaRequest.class))).thenReturn(reservaConfirmada());

        mockMvc.perform(crear())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value(ReservaService.MSG_RESERVA_CONFIRMADA))
                .andExpect(jsonPath("$.codigoReserva").value("RSV-00000001"))
                .andExpect(jsonPath("$.estado").value("Confirmada"))
                .andExpect(jsonPath("$.resumen").value(
                        "\"Corte clásico\", \"Barbería Centro\", \"Carlos Ruiz\", lunes 09/11/2026, 09:00-09:30"));

        ArgumentCaptor<CrearReservaRequest> captor = ArgumentCaptor.forClass(CrearReservaRequest.class);
        verify(reservaService).crearReserva(captor.capture());
        CrearReservaRequest enviada = captor.getValue();
        assertEquals(1, enviada.getServicioId());
        assertEquals(12, enviada.getProveedorId());
        assertEquals(LUNES, enviada.getFecha());
        assertEquals(LocalTime.of(9, 0), enviada.getHoraInicio());
        assertEquals("Corte bajo", enviada.getNotas());
    }

    // HU-0018/CA-03
    @Test
    void debeResponder409_cuandoOtroClienteGanaLaFranjaAlMismoTiempo() throws Exception {
        when(reservaService.crearReserva(any(CrearReservaRequest.class)))
                .thenThrow(new RecursoDuplicadoException(ReservaService.MSG_FRANJA_RESERVADA));

        mockMvc.perform(crear())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(ReservaService.MSG_FRANJA_RESERVADA));
    }

    // HU-0018/CA-04
    @Test
    void debeResponder409_cuandoLaFranjaFueTomadaMientrasElClienteDecidia() throws Exception {
        when(reservaService.crearReserva(any(CrearReservaRequest.class)))
                .thenThrow(new RecursoDuplicadoException(ReservaValidator.MSG_FRANJA_RESERVADA));

        mockMvc.perform(crear())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(ReservaValidator.MSG_FRANJA_RESERVADA));
    }

    // HU-0018/CA-05
    @Test
    void debeResponder400_cuandoElClienteYaTieneUnaReservaEnEseHorario() throws Exception {
        when(reservaService.crearReserva(any(CrearReservaRequest.class)))
                .thenThrow(new SolicitudInvalidaException(ReservaValidator.MSG_CRUCE_CLIENTE));

        mockMvc.perform(crear())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ReservaValidator.MSG_CRUCE_CLIENTE));
    }

    // HU-0018/CA-06
    @Test
    void debeResponder400_cuandoLasNotasSuperan250Caracteres() throws Exception {
        when(reservaService.crearReserva(any(CrearReservaRequest.class)))
                .thenThrow(new SolicitudInvalidaException(ReservaValidator.MSG_NOTAS_LARGAS));

        mockMvc.perform(crear())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ReservaValidator.MSG_NOTAS_LARGAS));
    }

    // HU-0018/CA-07
    @Test
    void debeResponder403_cuandoUnProveedorIntentaReservar() throws Exception {
        when(reservaService.crearReserva(any(CrearReservaRequest.class)))
                .thenThrow(new AccesoDenegadoException(ReservaValidator.MSG_SOLO_CLIENTES));

        mockMvc.perform(crear())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(ReservaValidator.MSG_SOLO_CLIENTES));
    }

    // HU-0018/CA-08
    @Test
    void debeResponder400_cuandoElHorarioNoEsUnaFranjaDefinida() throws Exception {
        when(reservaService.crearReserva(any(CrearReservaRequest.class)))
                .thenThrow(new SolicitudInvalidaException(ReservaValidator.MSG_HORARIO_NO_DISPONIBLE));

        mockMvc.perform(crear())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ReservaValidator.MSG_HORARIO_NO_DISPONIBLE));
    }

    // HU-0018/CA-09
    @Test
    void debeResponder400_cuandoLaFranjaFueEliminada() throws Exception {
        when(reservaService.crearReserva(any(CrearReservaRequest.class)))
                .thenThrow(new SolicitudInvalidaException(ReservaValidator.MSG_FRANJA_NO_DISPONIBLE));

        mockMvc.perform(crear())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ReservaValidator.MSG_FRANJA_NO_DISPONIBLE));
    }

    @Test
    void debeResponder404_cuandoElServicioAReservarNoExiste() throws Exception {
        when(reservaService.crearReserva(any(CrearReservaRequest.class)))
                .thenThrow(new RecursoNoEncontradoException(ReservaService.MSG_SERVICIO_NO_ENCONTRADO));

        mockMvc.perform(crear())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(ReservaService.MSG_SERVICIO_NO_ENCONTRADO));
    }

    @Test
    void debeResponder400_cuandoLaFechaDeLaReservaNoTieneFormatoValido() throws Exception {
        mockMvc.perform(post(RESERVAS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"servicioId\":1,\"fecha\":\"09/11/2026\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El cuerpo de la solicitud no tiene un formato válido."));

        verifyNoInteractions(reservaService);
    }

    // HU-0016/CA-01
    @Test
    void debeResponder200ConLasFranjas_cuandoHayDisponibilidad() throws Exception {
        when(disponibilidadService.consultarFranjasDisponibles(1, LUNES, null)).thenReturn(List.of(
                franja("08:00", "08:30", "Carlos Ruiz"),
                franja("08:30", "09:00", "Carlos Ruiz")));

        mockMvc.perform(get(FRANJAS, 1).param("fecha", "2026-11-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].horaInicio").value("08:00"))
                .andExpect(jsonPath("$[0].horaFin").value("08:30"))
                .andExpect(jsonPath("$[0].nombreProveedor").value("Carlos Ruiz"));
    }

    @Test
    void debeFiltrarPorProveedor_cuandoSeEnviaElParametroProveedorId() throws Exception {
        when(disponibilidadService.consultarFranjasDisponibles(1, LUNES, 15))
                .thenReturn(List.of(franja("08:00", "08:30", "Diego Salas")));

        mockMvc.perform(get(FRANJAS, 1).param("fecha", "2026-11-09").param("proveedorId", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombreProveedor").value("Diego Salas"));
    }

    // HU-0016/CA-08
    @Test
    void debeResponder400ConMensaje_cuandoNoHayFranjasEseDia() throws Exception {
        LocalDate domingo = LocalDate.of(2026, 11, 15);
        when(disponibilidadService.consultarFranjasDisponibles(1, domingo, null))
                .thenThrow(new SolicitudInvalidaException(DisponibilidadService.MSG_SIN_FRANJAS_DISPONIBLES));

        mockMvc.perform(get(FRANJAS, 1).param("fecha", "2026-11-15"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(DisponibilidadService.MSG_SIN_FRANJAS_DISPONIBLES));
    }

    // HU-0016/CA-09
    @Test
    void debeResponder400ConMensaje_cuandoLaFechaEstaFueraDeLaVentana() throws Exception {
        LocalDate diciembre = LocalDate.of(2026, 12, 7);
        when(disponibilidadService.consultarFranjasDisponibles(1, diciembre, null))
                .thenThrow(new SolicitudInvalidaException(DisponibilidadService.MSG_VENTANA_FECHA_INVALIDA));

        mockMvc.perform(get(FRANJAS, 1).param("fecha", "2026-12-07"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(DisponibilidadService.MSG_VENTANA_FECHA_INVALIDA));
    }

    // HU-0016/CA-10
    @Test
    void debeResponder400ConMensaje_cuandoElServicioNoTieneDisponibilidad() throws Exception {
        when(disponibilidadService.consultarFranjasDisponibles(1, LUNES, null))
                .thenThrow(new SolicitudInvalidaException(DisponibilidadService.MSG_SERVICIO_SIN_DISPONIBILIDAD));

        mockMvc.perform(get(FRANJAS, 1).param("fecha", "2026-11-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(DisponibilidadService.MSG_SERVICIO_SIN_DISPONIBILIDAD));
    }

    @Test
    void debeResponder404_cuandoSeConsultanFranjasDeUnServicioInexistente() throws Exception {
        when(disponibilidadService.consultarFranjasDisponibles(99, LUNES, null))
                .thenThrow(new RecursoNoEncontradoException(DisponibilidadService.MSG_SERVICIO_NO_ENCONTRADO));

        mockMvc.perform(get(FRANJAS, 99).param("fecha", "2026-11-09"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(DisponibilidadService.MSG_SERVICIO_NO_ENCONTRADO));
    }

    @Test
    void debeResponder400_cuandoElIdDelServicioNoEsNumerico() throws Exception {
        mockMvc.perform(get(FRANJAS, "abc").param("fecha", "2026-11-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'servicioId' tiene un formato inválido."));

        verifyNoInteractions(disponibilidadService);
    }

    @Test
    void debeResponder400_cuandoLaFechaConsultadaNoTieneFormatoIso() throws Exception {
        mockMvc.perform(get(FRANJAS, 1).param("fecha", "09/11/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'fecha' tiene un formato inválido."));

        verifyNoInteractions(disponibilidadService);
    }

    // HU-0021/CA-01
    @Test
    void debeResponder200ConMensajeDeCancelacion_cuandoElClienteCancelaATiempo() throws Exception {
        ReservaResponse cancelada = reservaConfirmada();
        cancelada.setEstado("Cancelada");
        cancelada.setMensaje(ReservaService.MSG_RESERVA_CANCELADA);
        cancelada.setMotivoCancelacion("Viaje de trabajo");
        when(reservaService.cancelarReserva(eq(100), any(CancelarReservaRequest.class))).thenReturn(cancelada);

        mockMvc.perform(cancelar(100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(ReservaService.MSG_RESERVA_CANCELADA))
                .andExpect(jsonPath("$.estado").value("Cancelada"))
                .andExpect(jsonPath("$.motivoCancelacion").value("Viaje de trabajo"));

        ArgumentCaptor<CancelarReservaRequest> captor = ArgumentCaptor.forClass(CancelarReservaRequest.class);
        verify(reservaService).cancelarReserva(eq(100), captor.capture());
        assertEquals("Viaje de trabajo", captor.getValue().getMotivo());
        assertTrue(captor.getValue().getConfirmacion());
    }

    // HU-0021/CA-03
    @Test
    void debeResponder400_cuandoSeCancelaFueraDelPlazo() throws Exception {
        when(reservaService.cancelarReserva(eq(100), any(CancelarReservaRequest.class)))
                .thenThrow(new SolicitudInvalidaException(CancelacionReservaValidator.MSG_FUERA_DE_PLAZO));

        mockMvc.perform(cancelar(100))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(CancelacionReservaValidator.MSG_FUERA_DE_PLAZO));
    }

    // HU-0021/CA-05
    @Test
    void debeResponder400_cuandoLaReservaYaEstabaCancelada() throws Exception {
        when(reservaService.cancelarReserva(eq(100), any(CancelarReservaRequest.class)))
                .thenThrow(new SolicitudInvalidaException(CancelacionReservaValidator.MSG_RESERVA_YA_CANCELADA));

        mockMvc.perform(cancelar(100))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(CancelacionReservaValidator.MSG_RESERVA_YA_CANCELADA));
    }

    // HU-0021/CA-06
    @Test
    void debeResponder403_cuandoSeIntentaCancelarLaReservaDeOtraPersona() throws Exception {
        when(reservaService.cancelarReserva(eq(100), any(CancelarReservaRequest.class)))
                .thenThrow(new AccesoDenegadoException(CancelacionReservaValidator.MSG_SIN_PERMISO));

        mockMvc.perform(cancelar(100))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(CancelacionReservaValidator.MSG_SIN_PERMISO));
    }

    @Test
    void debeResponder404_cuandoSeCancelaUnaReservaInexistente() throws Exception {
        when(reservaService.cancelarReserva(eq(999), any(CancelarReservaRequest.class)))
                .thenThrow(new RecursoNoEncontradoException(ReservaService.MSG_RESERVA_NO_ENCONTRADA));

        mockMvc.perform(cancelar(999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(ReservaService.MSG_RESERVA_NO_ENCONTRADA));
    }

    @Test
    void debeResponder400_cuandoLaCancelacionNoTraeCuerpo() throws Exception {
        mockMvc.perform(post(CANCELAR, 100).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El cuerpo de la solicitud no tiene un formato válido."));

        verifyNoInteractions(reservaService);
    }

    @Test
    void debeResponder200ConElDetalle_cuandoSeConsultaPorId() throws Exception {
        when(reservaService.consultarPorId(100)).thenReturn(reservaConfirmada());

        mockMvc.perform(get(RESERVAS + "/{idReserva}", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReserva").value(100))
                .andExpect(jsonPath("$.nombreNegocio").value("Barbería Centro"));
    }

    @Test
    void debeResponder404_cuandoLaReservaPorIdNoSePuedeVer() throws Exception {
        when(reservaService.consultarPorId(200))
                .thenThrow(new RecursoNoEncontradoException(ReservaService.MSG_RESERVA_NO_ENCONTRADA));

        mockMvc.perform(get(RESERVAS + "/{idReserva}", 200))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(ReservaService.MSG_RESERVA_NO_ENCONTRADA));
    }

    @Test
    void debeResponder200ConElDetalle_cuandoSeConsultaPorCodigo() throws Exception {
        when(reservaService.consultarPorCodigo("RSV-00000001")).thenReturn(reservaConfirmada());

        mockMvc.perform(get(RESERVAS + "/codigo/{codigo}", "RSV-00000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value("RSV-00000001"));
    }

    @Test
    void debeResponder404_cuandoElCodigoNoCorrespondeAUnaReservaVisible() throws Exception {
        when(reservaService.consultarPorCodigo("RSV-99999999"))
                .thenThrow(new RecursoNoEncontradoException(ReservaService.MSG_RESERVA_NO_ENCONTRADA));

        mockMvc.perform(get(RESERVAS + "/codigo/{codigo}", "RSV-99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(ReservaService.MSG_RESERVA_NO_ENCONTRADA));
    }

    private MockHttpServletRequestBuilder crear() {
        return post(RESERVAS).contentType(MediaType.APPLICATION_JSON).content(CUERPO_RESERVA);
    }

    private MockHttpServletRequestBuilder cancelar(Integer idReserva) {
        return post(CANCELAR, idReserva).contentType(MediaType.APPLICATION_JSON).content(CUERPO_CANCELAR);
    }

    private ReservaResponse reservaConfirmada() {
        return ReservaResponse.builder()
                .mensaje(ReservaService.MSG_RESERVA_CONFIRMADA)
                .idReserva(100)
                .codigoReserva("RSV-00000001")
                .estado("Confirmada")
                .nombreServicio("Corte clásico")
                .nombreNegocio("Barbería Centro")
                .nombreProveedor("Carlos Ruiz")
                .fecha("2026-11-09")
                .horaInicio("09:00")
                .horaFin("09:30")
                .resumen("\"Corte clásico\", \"Barbería Centro\", \"Carlos Ruiz\", lunes 09/11/2026, 09:00-09:30")
                .build();
    }

    private FranjaDisponibleResponse franja(String inicio, String fin, String proveedor) {
        return FranjaDisponibleResponse.builder()
                .servicioId(1)
                .nombreServicio("Corte clásico")
                .nombreProveedor(proveedor)
                .fecha("2026-11-09")
                .horaInicio(inicio)
                .horaFin(fin)
                .build();
    }
}
