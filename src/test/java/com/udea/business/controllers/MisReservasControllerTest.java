package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.MisReservasResponse;
import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.controllers.DTOs.ReservaResumenResponse;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.services.IMisReservasService;
import com.udea.business.services.MisReservasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MisReservasControllerTest {

    private static final String URL = "/api/v1/reservas/mis-reservas";

    @Mock
    private IMisReservasService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MisReservasController(service))
                .setControllerAdvice(new ProveedoresExceptionHandler())
                .build();
    }

    // HU-0019/CA-01
    @Test
    void debeResponder200ConProximasEHistorial_cuandoElClienteTieneReservas() throws Exception {
        when(service.consultarMisReservas()).thenReturn(MisReservasResponse.builder()
                .proximas(List.of(resumen(1, "2026-11-09", "Confirmada"), resumen(2, "2026-11-12", "Confirmada")))
                .historial(List.of(resumen(3, "2026-11-02", "Realizada"), resumen(4, "2026-11-10", "Cancelada")))
                .build());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximas[0].fecha").value("2026-11-09"))
                .andExpect(jsonPath("$.proximas[1].fecha").value("2026-11-12"))
                .andExpect(jsonPath("$.historial[0].estado").value("Realizada"))
                .andExpect(jsonPath("$.historial[1].estado").value("Cancelada"))
                .andExpect(jsonPath("$.proximas[0].nombreServicio").value("Corte clásico"))
                .andExpect(jsonPath("$.proximas[0].nombreNegocio").value("Barbería Centro"))
                .andExpect(jsonPath("$.mensaje").doesNotExist());
    }

    // HU-0019/CA-06
    @Test
    void debeResponder200ConMensaje_cuandoElClienteNoTieneReservas() throws Exception {
        when(service.consultarMisReservas()).thenReturn(MisReservasResponse.builder()
                .mensaje(MisReservasService.MSG_SIN_RESERVAS)
                .proximas(List.of())
                .historial(List.of())
                .build());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(MisReservasService.MSG_SIN_RESERVAS))
                .andExpect(jsonPath("$.historial").isEmpty());
    }

    @Test
    void debeResponder403_cuandoQuienConsultaNoEsCliente() throws Exception {
        when(service.consultarMisReservas())
                .thenThrow(new AccesoDenegadoException(MisReservasService.MSG_SOLO_CLIENTES));

        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(MisReservasService.MSG_SOLO_CLIENTES));
    }

    // HU-0019/CA-04
    @Test
    void debeResponder200ConElDetalle_cuandoLaReservaEsDelCliente() throws Exception {
        when(service.consultarDetalle(100)).thenReturn(ReservaResponse.builder()
                .idReserva(100)
                .codigoReserva("RSV-00000001")
                .estado("Confirmada")
                .nombreNegocio("Barbería Centro")
                .nombreServicio("Corte clásico")
                .nombreProveedor("Carlos Ruiz")
                .fecha("2026-11-09")
                .horaInicio("09:00")
                .horaFin("09:30")
                .duracionMinutos(30)
                .build());

        mockMvc.perform(get(URL + "/{idReserva}", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value("RSV-00000001"))
                .andExpect(jsonPath("$.estado").value("Confirmada"))
                .andExpect(jsonPath("$.nombreProveedor").value("Carlos Ruiz"))
                .andExpect(jsonPath("$.duracionMinutos").value(30));
    }

    // HU-0019/CA-05
    @Test
    void debeResponder200ConMotivo_cuandoElDetalleEsDeUnaReservaCancelada() throws Exception {
        when(service.consultarDetalle(101)).thenReturn(ReservaResponse.builder()
                .idReserva(101)
                .estado("Cancelada")
                .motivoCancelacion("Viaje de trabajo")
                .build());

        mockMvc.perform(get(URL + "/{idReserva}", 101))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("Cancelada"))
                .andExpect(jsonPath("$.motivoCancelacion").value("Viaje de trabajo"));
    }

    // HU-0019/CA-07
    @Test
    void debeResponder404_cuandoElDetalleEsDeOtroCliente() throws Exception {
        when(service.consultarDetalle(200))
                .thenThrow(new RecursoNoEncontradoException(MisReservasService.MSG_RESERVA_NO_ENCONTRADA));

        mockMvc.perform(get(URL + "/{idReserva}", 200))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(MisReservasService.MSG_RESERVA_NO_ENCONTRADA));
    }

    @Test
    void debeResponder400_cuandoElIdDeLaReservaNoEsNumerico() throws Exception {
        mockMvc.perform(get(URL + "/{idReserva}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'idReserva' tiene un formato inválido."));

        verifyNoInteractions(service);
    }

    private ReservaResumenResponse resumen(Integer id, String fecha, String estado) {
        return ReservaResumenResponse.builder()
                .idReserva(id)
                .fecha(fecha)
                .horaInicio("09:00")
                .horaFin("09:30")
                .nombreServicio("Corte clásico")
                .nombreNegocio("Barbería Centro")
                .estado(estado)
                .build();
    }
}
