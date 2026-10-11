package com.udea.business.services;

import com.udea.business.controllers.DTOs.NegocioCreateRequest;
import com.udea.business.entities.Negocio;

import java.util.List;

public interface INegocioService {
    Negocio crearNegocio(NegocioCreateRequest request, Integer usuarioIdAutenticado, boolean esPropietario);
    Negocio crearNegocio(NegocioCreateRequest request, Integer usuarioIdAutenticado);
    List<Negocio> listarMisNegocios(Integer usuarioIdAutenticado, boolean esPropietario);
    List<Negocio> listarMisNegocios(Integer usuarioIdAutenticado);
}
