package com.udea.business.services;

import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.controllers.DTOs.ReservaResumenResponse;
import com.udea.business.entities.EstadoVisibleReserva;
import com.udea.business.entities.Reserva;

/**
 * Convierte reservas (con sus relaciones ya cargadas) en las vistas de consulta
 * del cliente (HU-0019).
 */
public interface IReservaConsultaMapper {

    ReservaResumenResponse aResumen(Reserva reserva, EstadoVisibleReserva estado);

    ReservaResponse aDetalle(Reserva reserva, EstadoVisibleReserva estado);
}
