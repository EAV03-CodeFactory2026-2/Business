package com.udea.business.controllers.DTOs;

import com.udea.business.entities.Recurso;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecursoResponse {

    private Integer idRecurso;
    private Integer negocioId;
    private String nombre;
    private String tipo;
    private Integer cantidad;
    private String descripcion;
    private String estado;

    public static RecursoResponse desde(Recurso recurso) {
        return RecursoResponse.builder()
                .idRecurso(recurso.getIdRecurso())
                .negocioId(recurso.getNegocio() != null ? recurso.getNegocio().getIdNegocio() : null)
                .nombre(recurso.getNombre())
                .tipo(recurso.getTipo())
                .cantidad(recurso.getCantidad())
                .descripcion(recurso.getDescripcion())
                .estado(recurso.getEstado())
                .build();
    }
}
