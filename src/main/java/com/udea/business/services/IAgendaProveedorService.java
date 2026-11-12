package com.udea.business.services;

import com.udea.business.controllers.DTOs.AgendaProveedorResponse;

import java.time.LocalDate;

/**
 * HU-0022 · Consultar mi agenda diaria de proveedor.
 */
public interface IAgendaProveedorService {

    /**
     * @param fecha día a consultar; si es {@code null} se usa el día actual.
     */
    AgendaProveedorResponse consultarMiAgenda(LocalDate fecha);
}
