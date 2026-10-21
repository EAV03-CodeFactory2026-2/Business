package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.AsignacionProveedoresResponse;
import com.udea.business.controllers.DTOs.AsignarProveedoresRequest;
import com.udea.business.controllers.DTOs.ProveedorAsignadoResponse;
import com.udea.business.entities.EstadoServicio;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.services.IAsignacionProveedorService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static com.udea.business.services.AsignacionProveedorService.MSG_ASIGNADO;
import static com.udea.business.services.AsignacionProveedorService.MSG_REPETIDOS;
import static com.udea.business.services.AsignacionProveedorService.MSG_SERVICIO_NO_ENCONTRADO;
import static com.udea.business.services.AsignacionProveedorService.MSG_SIN_PERMISO;
import static com.udea.business.services.AsignacionProveedorService.MSG_SIN_SELECCION;
import static com.udea.business.services.AsignacionProveedorService.MSG_YA_VINCULADOS;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AsignacionProveedorControllerTest {

    private static final String URL = "/api/v1/servicios/{servicioId}/proveedores";
    private static final Integer SERVICIO_ID = 10;

    @Mock
    private IAsignacionProveedorService service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AsignacionProveedorController(service))
                .setControllerAdvice(new ProveedoresExceptionHandler())
                .build();
    }

    @Test
    void asignarRetorna201ConLosProveedoresVinculados() throws Exception {
        ProveedorAsignadoResponse ana = ProveedorAsignadoResponse.builder()
                .idUsuario(12)
                .nombre("Ana")
                .apellido("Gomez")
                .especialidad("Masajes")
                .tipoDocumento("CC")
                .numeroDocumento("1001")
                .build();
        ProveedorAsignadoResponse luis = ProveedorAsignadoResponse.builder()
                .idUsuario(15)
                .nombre("Luis")
                .apellido("Perez")
                .especialidad("Masajes")
                .tipoDocumento("CC")
                .numeroDocumento("1002")
                .build();
        AsignacionProveedoresResponse respuesta = AsignacionProveedoresResponse.builder()
                .mensaje(String.format(MSG_ASIGNADO, 2))
                .idServicio(SERVICIO_ID)
                .estadoServicio(EstadoServicio.ASIGNADO)
                .proveedoresAsignados(List.of(ana, luis))
                .totalProveedores(2)
                .build();
        when(service.asignar(SERVICIO_ID, List.of(12, 15))).thenReturn(respuesta);

        mockMvc.perform(conProveedores(12, 15))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value(String.format(MSG_ASIGNADO, 2)))
                .andExpect(jsonPath("$.idServicio").value(SERVICIO_ID))
                .andExpect(jsonPath("$.estadoServicio").value(EstadoServicio.ASIGNADO))
                .andExpect(jsonPath("$.totalProveedores").value(2))
                .andExpect(jsonPath("$.proveedoresAsignados.length()").value(2))
                .andExpect(jsonPath("$.proveedoresAsignados[0].idUsuario").value(12))
                .andExpect(jsonPath("$.proveedoresAsignados[0].nombre").value("Ana"))
                .andExpect(jsonPath("$.proveedoresAsignados[0].tipoDocumento").value("CC"))
                .andExpect(jsonPath("$.proveedoresAsignados[1].idUsuario").value(15))
                .andExpect(jsonPath("$.proveedoresAsignados[1].numeroDocumento").value("1002"));

        verify(service).asignar(SERVICIO_ID, List.of(12, 15));
    }

    @Test
    void asignarEnviaSeleccionNulaAlServiceCuandoNoHayBody() throws Exception {
        when(service.asignar(SERVICIO_ID, null)).thenThrow(new SolicitudInvalidaException(MSG_SIN_SELECCION));

        mockMvc.perform(post(URL, SERVICIO_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(MSG_SIN_SELECCION));

        verify(service).asignar(SERVICIO_ID, null);
    }

    @Test
    void asignarRetorna403CuandoElServiceLanzaAccesoDenegado() throws Exception {
        when(service.asignar(SERVICIO_ID, List.of(12))).thenThrow(new AccesoDenegadoException(MSG_SIN_PERMISO));

        mockMvc.perform(conProveedores(12))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(MSG_SIN_PERMISO));
    }

    @Test
    void asignarRetorna404CuandoElServiceLanzaRecursoNoEncontrado() throws Exception {
        when(service.asignar(SERVICIO_ID, List.of(12)))
                .thenThrow(new RecursoNoEncontradoException(MSG_SERVICIO_NO_ENCONTRADO));

        mockMvc.perform(conProveedores(12))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(MSG_SERVICIO_NO_ENCONTRADO));
    }

    @Test
    void asignarRetorna409CuandoElServiceLanzaRecursoDuplicado() throws Exception {
        String mensaje = String.format(MSG_YA_VINCULADOS, "Ana Gomez (CC 1001)");
        when(service.asignar(SERVICIO_ID, List.of(12))).thenThrow(new RecursoDuplicadoException(mensaje));

        mockMvc.perform(conProveedores(12))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(mensaje));
    }

    @Test
    void asignarRetorna400CuandoElServiceLanzaSolicitudInvalida() throws Exception {
        when(service.asignar(SERVICIO_ID, List.of(12, 12))).thenThrow(new SolicitudInvalidaException(MSG_REPETIDOS));

        mockMvc.perform(conProveedores(12, 12))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(MSG_REPETIDOS));
    }

    @Test
    void asignarRetorna400CuandoElServicioIdNoEsNumerico() throws Exception {
        String body = objectMapper.writeValueAsString(new AsignarProveedoresRequest(List.of(12)));

        mockMvc.perform(post(URL, "abc").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El parámetro 'servicioId' tiene un formato inválido."));

        verifyNoInteractions(service);
    }

    @Test
    void asignarRetorna400CuandoElJsonEstaMalformado() throws Exception {
        mockMvc.perform(post(URL, SERVICIO_ID).contentType(MediaType.APPLICATION_JSON).content("{\"proveedorIds\": [12,"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El cuerpo de la solicitud no tiene un formato válido."));

        verifyNoInteractions(service);
    }

    @Test
    void asignarNoTraduceLasExcepcionesNoControladasDelService() {
        IllegalStateException fallo = new IllegalStateException("detalle interno");
        when(service.asignar(SERVICIO_ID, List.of(12))).thenThrow(fallo);
        MockHttpServletRequestBuilder peticion = conProveedores(12);

        ServletException ex = assertThrows(ServletException.class, () -> mockMvc.perform(peticion));

        assertSame(fallo, ex.getCause());
    }

    private MockHttpServletRequestBuilder conProveedores(Integer... ids) {
        String body = objectMapper.writeValueAsString(new AsignarProveedoresRequest(List.of(ids)));
        return post(URL, SERVICIO_ID).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
