package com.udea.business.services;

import com.udea.business.controllers.DTOs.ServicioCreateRequest;
import com.udea.business.entities.Servicio;

import java.util.List;

public interface IServicioService {
    Servicio crearServicio(ServicioCreateRequest request, Integer usuarioIdAutenticado, boolean esPropietario);
    Servicio crearServicio(ServicioCreateRequest request, Integer usuarioIdAutenticado);
    List<Servicio> listarServiciosPorNegocio(Integer negocioId);
}
