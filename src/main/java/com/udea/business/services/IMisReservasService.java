package com.udea.business.services;

import com.udea.business.controllers.DTOs.MisReservasResponse;
import com.udea.business.controllers.DTOs.ReservaResponse;

/**
 * HU-0019 · Consultar mis reservas y su detalle (cliente autenticado).
 */
public interface IMisReservasService {

    MisReservasResponse consultarMisReservas();

    ReservaResponse consultarDetalle(Integer idReserva);
}
