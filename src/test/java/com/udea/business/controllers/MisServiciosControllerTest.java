package com.udea.business.controllers;

import com.udea.business.controllers.DTOs.ServicioAsignadoResponse;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.services.IAsignacionProveedorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static com.udea.business.services.AsignacionProveedorService.MSG_SOLO_PROVEEDOR;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MisServiciosControllerTest {

    private static final String URL = "/api/v1/proveedores/mis-servicios";

    @Mock
    private IAsignacionProveedorService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MisServiciosController(service))
                .setControllerAdvice(new ProveedoresExceptionHandler())
                .build();
    }

    @Test
    void consultarMisServiciosRetorna200ConServicios() throws Exception {
        ServicioAsignadoResponse servicio = ServicioAsignadoResponse.builder()
                .idServicio(10)
                .nombre("Corte de Cabello")
                .descripcion("Corte estilo clásico")
                .duracionMinutos(30)
                .precio(BigDecimal.valueOf(25000))
                .estado("Asignado")
                .idNegocio(1)
                .nombreNegocio("Barbería Central")
                .build();

        when(service.consultarMisServicios()).thenReturn(List.of(servicio));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idServicio").value(10))
                .andExpect(jsonPath("$[0].nombre").value("Corte de Cabello"))
                .andExpect(jsonPath("$[0].descripcion").value("Corte estilo clásico"))
                .andExpect(jsonPath("$[0].duracionMinutos").value(30))
                .andExpect(jsonPath("$[0].precio").value(25000))
                .andExpect(jsonPath("$[0].idNegocio").value(1))
                .andExpect(jsonPath("$[0].nombreNegocio").value("Barbería Central"));

        verify(service).consultarMisServicios();
    }

    @Test
    void consultarMisServiciosRetorna200ConListaVacia() throws Exception {
        when(service.consultarMisServicios()).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(service).consultarMisServicios();
    }

    @Test
    void consultarMisServiciosRetorna403CuandoNoEsProveedor() throws Exception {
        when(service.consultarMisServicios()).thenThrow(new AccesoDenegadoException(MSG_SOLO_PROVEEDOR));

        mockMvc.perform(get(URL))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value(MSG_SOLO_PROVEEDOR));

        verify(service).consultarMisServicios();
    }
}
