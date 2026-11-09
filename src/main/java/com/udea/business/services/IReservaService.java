package com.udea.business.services;

import com.udea.business.controllers.DTOs.CancelarReservaRequest;
import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.controllers.DTOs.ReservaResponse;

import java.time.LocalDate;
import java.util.List;

public interface IReservaService {

    ReservaResponse crearReserva(CrearReservaRequest request);

    ReservaResponse consultarPorId(Integer idReserva);

    ReservaResponse consultarPorCodigo(String codigoReserva);

    List<ReservaResponse> consultarMisReservas();

    ReservaResponse cancelarReserva(Integer idReserva, CancelarReservaRequest request);

    List<ReservaResponse> consultarAgendaProveedor(Integer proveedorId, LocalDate fecha);

    List<ReservaResponse> consultarMiAgenda(LocalDate fecha);
}
