package com.udea.business.controllers.DTOs;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Las validaciones se hacen en AsignacionProveedorService, después de verificar permisos,
 * para que un usuario sin permiso siempre reciba 403.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsignarProveedoresRequest {

    @Schema(description = "Ids de usuario de los proveedores a vincular", example = "[12, 15]")
    private List<Integer> proveedorIds;
}
