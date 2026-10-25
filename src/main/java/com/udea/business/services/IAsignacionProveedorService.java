package com.udea.business.services;

import com.udea.business.controllers.DTOs.AsignacionProveedoresResponse;
import com.udea.business.controllers.DTOs.ProveedorAsignadoResponse;
import com.udea.business.controllers.DTOs.ServicioAsignadoResponse;

import java.util.List;

public interface IAsignacionProveedorService {
    AsignacionProveedoresResponse asignar(Integer servicioId, List<Integer> proveedorIds);
    List<ProveedorAsignadoResponse> consultarProveedoresAsignados(Integer servicioId);
    List<ServicioAsignadoResponse> consultarMisServicios();
}
