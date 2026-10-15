package com.udea.business.services;

import com.udea.business.controllers.DTOs.AsignacionProveedoresResponse;

import java.util.List;

public interface IAsignacionProveedorService {
    AsignacionProveedoresResponse asignar(Integer servicioId, List<Integer> proveedorIds);
}
