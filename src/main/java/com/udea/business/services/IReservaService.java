package com.udea.business.services;

import com.udea.business.controllers.DTOs.CancelarReservaRequest;
import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.controllers.DTOs.ReservaResponse;

public interface IReservaService {

    ReservaResponse crearReserva(CrearReservaRequest request);

    ReservaResponse consultarPorId(Integer idReserva);

    ReservaResponse consultarPorCodigo(String codigoReserva);

    ReservaResponse cancelarReserva(Integer idReserva, CancelarReservaRequest request);
}
