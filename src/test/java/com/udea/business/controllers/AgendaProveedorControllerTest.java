package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.AgendaProveedorResponse;
import com.udea.business.controllers.DTOs.ReservaAgendaResponse;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.services.AgendaProveedorService;
import com.udea.business.services.IAgendaProveedorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AgendaProveedorControllerTest {

    private static final String URL = "/api/v1/reservas/mi-agenda";
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);

    @Mock
    private IAgendaProveedorService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AgendaProveedorController(service))
                .setControllerAdvice(new ProveedoresExceptionHandler())
                .build();
    }

    // HU-0022/CA-01
    @Test
    void debeResponder200ConLasReservasDelDia_cuandoSeIndicaLaFecha() throws Exception {
        when(service.consultarMiAgenda(LUNES)).thenReturn(AgendaProveedorResponse.builder()
                .fecha("2026-11-09")
                .totalReservas(2)
                .reservas(List.of(
                        reserva("09:00", "Barbería Centro", "Laura Pérez", "+573001234567", "Prefiero corte con tijera"),
                        reserva("10:00", "Barbería Norte", "Mario Gil", null, null)))
                .build());

        mockMvc.perform(get(URL).param("fecha", "2026-11-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReservas").value(2))
                .andExpect(jsonPath("$.reservas[0].nombreCliente").value("Laura Pérez"))
                .andExpect(jsonPath("$.reservas[0].nombreNegocio").value("Barbería Centro"))
                .andExpect(jsonPath("$.reservas[1].horaInicio").value("10:00"))
                .andExpect(jsonPath("$.reservas[0].telefonoCliente").value("+573001234567"))
                .andExpect(jsonPath("$.reservas[0].notas").value("Prefiero corte con tijera"));
    }

    // HU-0022/CA-03
    @Test
    void debePedirLaAgendaDeHoy_cuandoNoSeIndicaFecha() throws Exception {
        when(service.consultarMiAgenda(null)).thenReturn(AgendaProveedorResponse.builder()
                .fecha("2026-11-09")
                .totalReservas(0)
                .mensaje(AgendaProveedorService.MSG_SIN_RESERVAS)
                .reservas(List.of())
                .build());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value("2026-11-09"));
    }

    // HU-0022/CA-05
    @Test
    void debeResponder200ConMensaje_cuandoNoHayReservasEseDia() throws Exception {
        LocalDate miercoles = LocalDate.of(2026, 11, 11);
        when(service.consultarMiAgenda(miercoles)).thenReturn(AgendaProveedorResponse.builder()
                .fecha("2026-11-11")
                .totalReservas(0)
                .mensaje(AgendaProveedorService.MSG_SIN_RESERVAS)
                .reservas(List.of())
                .build());

        mockMvc.perform(get(URL).param("fecha", "2026-11-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value(AgendaProveedorService.MSG_SIN_RESERVAS))
                .andExpect(jsonPath("$.totalReservas").value(0));
    }

    @Test
    void debeResponder403_cuandoQuienConsultaNoEsProveedor() throws Exception {
        when(service.consultarMiAgenda(LUNES))
                .thenThrow(new AccesoDenegadoException(AgendaProveedorService.MSG_SOLO_PROVEEDORES));

        mockMvc.perform(get(URL).param("fecha", "2026-11-09"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(AgendaProveedorService.MSG_SOLO_PROVEEDORES));
    }

    @Test
    void debeResponder400_cuandoLaFechaNoTieneFormatoValido() throws Exception {
        mockMvc.perform(get(URL).param("fecha", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'fecha' tiene un formato inválido."));

        verifyNoInteractions(service);
    }

    private ReservaAgendaResponse reserva(String hora, String negocio, String cliente, String telefono, String notas) {
        return ReservaAgendaResponse.builder()
                .horaInicio(hora)
                .nombreServicio("Corte clásico")
                .nombreNegocio(negocio)
                .nombreCliente(cliente)
                .telefonoCliente(telefono)
                .notas(notas)
                .build();
    }
}
