package com.udea.business.controllers.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignacionProveedoresResponse {

    private String mensaje;
    private Integer idServicio;
    private String estadoServicio;
    private List<ProveedorAsignadoResponse> proveedoresAsignados;
    private long totalProveedores;
}
