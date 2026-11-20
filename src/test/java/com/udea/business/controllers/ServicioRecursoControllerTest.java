package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.AsociacionRecursoResponse;
import com.udea.business.controllers.DTOs.AsociarRecursoRequest;
import com.udea.business.controllers.DTOs.RecursoRequeridoResponse;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.services.IServicioRecursoService;
import com.udea.business.services.ServicioRecursoService;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ServicioRecursoControllerTest {

    private static final String URL = "/api/v1/servicios/{servicioId}/recursos";
    private static final Integer SERVICIO_ID = 10;
    private static final Integer CAMILLA_ID = 30;

    @Mock
    private IServicioRecursoService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ServicioRecursoController(service))
                .setControllerAdvice(new ProveedoresExceptionHandler())
                .build();
    }

    // HU-0045/CA-01
    @Test
    void debeResponder201ConMensaje_cuandoSeAsociaLaCamilla() throws Exception {
        when(service.asociarRecurso(eq(SERVICIO_ID), any(AsociarRecursoRequest.class)))
                .thenReturn(AsociacionRecursoResponse.builder()
                        .mensaje(ServicioRecursoService.MSG_RECURSO_ASOCIADO)
                        .idServicio(SERVICIO_ID)
                        .recursosRequeridos(List.of(requerido(CAMILLA_ID, "Camilla", 1)))
                        .build());

        mockMvc.perform(asociar(CAMILLA_ID, 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value(ServicioRecursoService.MSG_RECURSO_ASOCIADO))
                .andExpect(jsonPath("$.recursosRequeridos[0].nombreRecurso").value("Camilla"))
                .andExpect(jsonPath("$.recursosRequeridos[0].cantidadRequerida").value(1));

        ArgumentCaptor<AsociarRecursoRequest> captor = ArgumentCaptor.forClass(AsociarRecursoRequest.class);
        verify(service).asociarRecurso(eq(SERVICIO_ID), captor.capture());
        assertEquals(CAMILLA_ID, captor.getValue().getRecursoId());
        assertEquals((short) 1, captor.getValue().getCantidadRequerida());
    }

    // HU-0045/CA-02
    @Test
    void debeResponder200ConLosRecursosRequeridos_cuandoSeConsultaElServicio() throws Exception {
        when(service.consultarRecursosRequeridos(SERVICIO_ID)).thenReturn(List.of(
                requerido(CAMILLA_ID, "Camilla", 2),
                requerido(31, "Sala privada", 1)));

        mockMvc.perform(get(URL, SERVICIO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].cantidadRequerida").value(2))
                .andExpect(jsonPath("$[1].nombreRecurso").value("Sala privada"));
    }

    // HU-0045/CA-03
    @Test
    void debeResponder400_cuandoSePidenMasUnidadesDeLasDisponibles() throws Exception {
        when(service.asociarRecurso(eq(SERVICIO_ID), any(AsociarRecursoRequest.class)))
                .thenThrow(new SolicitudInvalidaException("El recurso Camilla solo tiene 2 unidades."));

        mockMvc.perform(asociar(CAMILLA_ID, 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El recurso Camilla solo tiene 2 unidades."));
    }

    // HU-0045/CA-04
    @Test
    void debeResponder400_cuandoElRecursoEsDeOtroNegocio() throws Exception {
        when(service.asociarRecurso(eq(SERVICIO_ID), any(AsociarRecursoRequest.class)))
                .thenThrow(new SolicitudInvalidaException(ServicioRecursoService.MSG_RECURSO_OTRO_NEGOCIO));

        mockMvc.perform(asociar(32, 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(ServicioRecursoService.MSG_RECURSO_OTRO_NEGOCIO));
    }

    // HU-0045/CA-05
    @Test
    void debeResponder409_cuandoElRecursoYaEstaAsociado() throws Exception {
        when(service.asociarRecurso(eq(SERVICIO_ID), any(AsociarRecursoRequest.class)))
                .thenThrow(new RecursoDuplicadoException(ServicioRecursoService.MSG_RECURSO_YA_ASOCIADO));

        mockMvc.perform(asociar(CAMILLA_ID, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(ServicioRecursoService.MSG_RECURSO_YA_ASOCIADO));
    }

    // HU-0045/CA-06
    @Test
    void debeResponder204_cuandoSeQuitaLaCamillaDelServicio() throws Exception {
        mockMvc.perform(delete(URL + "/{recursoId}", SERVICIO_ID, CAMILLA_ID))
                .andExpect(status().isNoContent());

        verify(service).quitarRecurso(SERVICIO_ID, CAMILLA_ID);
    }

    @Test
    void debeResponder404_cuandoSeQuitaUnRecursoNoAsociado() throws Exception {
        doThrow(new RecursoNoEncontradoException(ServicioRecursoService.MSG_RECURSO_NO_ENCONTRADO))
                .when(service).quitarRecurso(SERVICIO_ID, 99);

        mockMvc.perform(delete(URL + "/{recursoId}", SERVICIO_ID, 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(ServicioRecursoService.MSG_RECURSO_NO_ENCONTRADO));
    }

    // HU-0045/CA-07
    @Test
    void debeResponder403_cuandoElServicioEsDeOtroPropietario() throws Exception {
        when(service.asociarRecurso(eq(SERVICIO_ID), any(AsociarRecursoRequest.class)))
                .thenThrow(new AccesoDenegadoException(ServicioRecursoService.MSG_SIN_PERMISO));

        mockMvc.perform(asociar(CAMILLA_ID, 1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(ServicioRecursoService.MSG_SIN_PERMISO));
    }

    @Test
    void debeResponder404_cuandoElServicioNoExiste() throws Exception {
        when(service.consultarRecursosRequeridos(99))
                .thenThrow(new RecursoNoEncontradoException(ServicioRecursoService.MSG_SERVICIO_NO_ENCONTRADO));

        mockMvc.perform(get(URL, 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(ServicioRecursoService.MSG_SERVICIO_NO_ENCONTRADO));
    }

    private MockHttpServletRequestBuilder asociar(Integer recursoId, int cantidad) {
        return post(URL, SERVICIO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recursoId\":" + recursoId + ",\"cantidadRequerida\":" + cantidad + "}");
    }

    private RecursoRequeridoResponse requerido(Integer id, String nombre, int cantidad) {
        return RecursoRequeridoResponse.builder()
                .idRecurso(id)
                .nombreRecurso(nombre)
                .cantidadRequerida((short) cantidad)
                .build();
    }
}
