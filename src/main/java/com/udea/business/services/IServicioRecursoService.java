package com.udea.business.services;

import com.udea.business.controllers.DTOs.AsociacionRecursoResponse;
import com.udea.business.controllers.DTOs.AsociarRecursoRequest;
import com.udea.business.controllers.DTOs.RecursoRequeridoResponse;

import java.util.List;

public interface IServicioRecursoService {

    AsociacionRecursoResponse asociarRecurso(Integer servicioId, AsociarRecursoRequest request);

    List<RecursoRequeridoResponse> consultarRecursosRequeridos(Integer servicioId);

    void quitarRecurso(Integer servicioId, Integer recursoId);
}
