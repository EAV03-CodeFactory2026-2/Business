package com.udea.business.controllers.DTOs;

import com.udea.business.entities.PerfilProveedor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Proveedor vinculado a un servicio. Incluye tipo y número de documento para diferenciar homónimos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorAsignadoResponse {

    private Integer idUsuario;
    private String nombre;
    private String apellido;
    private String especialidad;
    private String tipoDocumento;
    private String numeroDocumento;

    public static ProveedorAsignadoResponse desde(PerfilProveedor perfil) {
        return ProveedorAsignadoResponse.builder()
                .idUsuario(perfil.getUsuarioId())
                .nombre(perfil.getUsuario().getNombre())
                .apellido(perfil.getUsuario().getApellido())
                .especialidad(perfil.getEspecialidad())
                .tipoDocumento(perfil.getTipoDocumento().getCodigo())
                .numeroDocumento(perfil.getNumeroDocumento())
                .build();
    }
}
