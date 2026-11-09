package com.udea.business.services;

import com.udea.business.controllers.DTOs.CancelarReservaRequest;
import com.udea.business.entities.Reserva;
import com.udea.business.security.IUsuarioActual;

import java.time.LocalDateTime;

public interface ICancelacionReservaValidator {

    void validarRolCliente(IUsuarioActual usuarioActual);

    void validarPropiedadReserva(Reserva reserva, IUsuarioActual usuarioActual);

    void validarConfirmacion(Boolean confirmacion);

    void validarEstadoReserva(Reserva reserva);

    void validarPlazoCancelacion(Reserva reserva, LocalDateTime ahora);

    void validarMotivo(String motivo);

    void validarCancelacion(Reserva reserva, IUsuarioActual usuarioActual, CancelarReservaRequest request, LocalDateTime ahora);
    
    void validarCancelacion(Reserva reserva, IUsuarioActual usuarioActual, CancelarReservaRequest request);
}
