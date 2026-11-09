package com.udea.business.services;

import com.udea.business.controllers.DTOs.FranjaDisponibleResponse;

import java.time.LocalDate;
import java.util.List;

public interface IDisponibilidadService {
    List<FranjaDisponibleResponse> consultarFranjasDisponibles(Integer servicioId, LocalDate fecha, Integer proveedorId);
}
