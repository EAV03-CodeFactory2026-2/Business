package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.RecursoCreateRequest;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.Recurso;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.services.IRecursoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecursoControllerTest {

    private static final String URL = "/api/v1/negocios/5/recursos";
    private static final String BODY = """
            {"nombre": "Camilla de masajes", "tipo": "Equipo", "cantidad": 3}
            """;

    @Mock
    private IRecursoService recursoService;

    private MockMvc mockMvc;
    private final Principal principal = () -> "10";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RecursoController(recursoService)).build();
    }

    private Recurso camilla() {
        return Recurso.builder()
                .idRecurso(1)
                .negocio(Negocio.builder().idNegocio(5).build())
                .nombre("Camilla de masajes")
                .tipo("Equipo")
                .cantidad(3)
                .estado("Activo")
                .build();
    }

    @Test
    void registrarRecursoRetorna201ConMensaje() throws Exception {
        when(recursoService.registrarRecurso(eq(5), any(RecursoCreateRequest.class), eq(10))).thenReturn(camilla());

        mockMvc.perform(post(URL).principal(principal).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensaje").value("Recurso registrado correctamente."))
                .andExpect(jsonPath("$.recurso.idRecurso").value(1))
                .andExpect(jsonPath("$.recurso.negocioId").value(5))
                .andExpect(jsonPath("$.recurso.cantidad").value(3))
                .andExpect(jsonPath("$.recurso.estado").value("Activo"));
    }

    @Test
    void registrarRecursoRetorna400PorValidacion() throws Exception {
        when(recursoService.registrarRecurso(eq(5), any(RecursoCreateRequest.class), eq(10)))
                .thenThrow(new IllegalArgumentException("La cantidad debe ser un número entero entre 1 y 999."));

        mockMvc.perform(post(URL).principal(principal).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La cantidad debe ser un número entero entre 1 y 999."));
    }

    @Test
    void registrarRecursoRetorna403SinPermiso() throws Exception {
        when(recursoService.registrarRecurso(eq(5), any(RecursoCreateRequest.class), eq(10)))
                .thenThrow(new AccesoDenegadoException("No tienes permiso para gestionar los recursos de este negocio."));

        mockMvc.perform(post(URL).principal(principal).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No tienes permiso para gestionar los recursos de este negocio."));
    }

    @Test
    void registrarRecursoRetorna404SiNegocioNoExiste() throws Exception {
        when(recursoService.registrarRecurso(eq(5), any(RecursoCreateRequest.class), eq(10)))
                .thenThrow(new RecursoNoEncontradoException("Negocio no encontrado."));

        mockMvc.perform(post(URL).principal(principal).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void registrarRecursoRetorna409PorNombreDuplicado() throws Exception {
        when(recursoService.registrarRecurso(eq(5), any(RecursoCreateRequest.class), eq(10)))
                .thenThrow(new RecursoDuplicadoException("Ya existe un recurso con este nombre en el negocio."));

        mockMvc.perform(post(URL).principal(principal).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Ya existe un recurso con este nombre en el negocio."));
    }

    @Test
    void listarRecursosVacioRetornaListaVaciaYMensaje() throws Exception {
        when(recursoService.listarRecursos(5, 10)).thenReturn(List.of());

        mockMvc.perform(get(URL).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Este negocio aún no tiene recursos registrados."))
                .andExpect(jsonPath("$.recursos").isArray())
                .andExpect(jsonPath("$.recursos").isEmpty());
    }

    @Test
    void listarRecursosRetornaLosRecursos() throws Exception {
        when(recursoService.listarRecursos(5, 10)).thenReturn(List.of(camilla()));

        mockMvc.perform(get(URL).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Recursos consultados correctamente."))
                .andExpect(jsonPath("$.recursos[0].nombre").value("Camilla de masajes"))
                .andExpect(jsonPath("$.recursos[0].cantidad").value(3));
    }

    @Test
    void listarRecursosRetorna403SinRolPropietario() throws Exception {
        when(recursoService.listarRecursos(5, 10))
                .thenThrow(new AccesoDenegadoException("Solo un Propietario puede gestionar los recursos del negocio."));

        mockMvc.perform(get(URL).principal(principal))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Solo un Propietario puede gestionar los recursos del negocio."));
    }
}
