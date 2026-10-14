package com.udea.business.services;

import com.udea.business.controllers.DTOs.RecursoCreateRequest;
import com.udea.business.entities.Recurso;

import java.util.List;

public interface IRecursoService {
    Recurso registrarRecurso(Integer negocioId, RecursoCreateRequest request, Integer usuarioIdAutenticado, boolean esPropietario);
    Recurso registrarRecurso(Integer negocioId, RecursoCreateRequest request, Integer usuarioIdAutenticado);
    List<Recurso> listarRecursos(Integer negocioId, Integer usuarioIdAutenticado, boolean esPropietario);
    List<Recurso> listarRecursos(Integer negocioId, Integer usuarioIdAutenticado);
}
