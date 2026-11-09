package com.udea.business.services;

import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.controllers.DTOs.ReservaResponse;

import java.util.List;

public interface IReservaService {

    ReservaResponse crearReserva(CrearReservaRequest request);

    ReservaResponse consultarPorId(Integer idReserva);

    ReservaResponse consultarPorCodigo(String codigoReserva);

    List<ReservaResponse> consultarMisReservas();
}
