package com.udea.business.controllers.DTOs;

import com.udea.business.entities.Servicio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioAsignadoResponse {

    private Integer idServicio;
    private String nombre;
    private String descripcion;
    private Integer duracionMinutos;
    private BigDecimal precio;
    private String estado;
    private Integer idNegocio;
    private String nombreNegocio;

    public static ServicioAsignadoResponse desde(Servicio servicio) {
        return ServicioAsignadoResponse.builder()
                .idServicio(servicio.getIdServicio())
                .nombre(servicio.getNombre())
                .descripcion(servicio.getDescripcion())
                .duracionMinutos(servicio.getDuracionMinutos())
                .precio(servicio.getPrecio())
                .estado(servicio.getEstado())
                .idNegocio(servicio.getNegocio() != null ? servicio.getNegocio().getIdNegocio() : null)
                .nombreNegocio(servicio.getNegocio() != null ? servicio.getNegocio().getNombreNegocio() : null)
                .build();
    }
}
