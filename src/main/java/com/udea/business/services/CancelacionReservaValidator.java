package com.udea.business.services;

import com.udea.business.controllers.DTOs.CancelarReservaRequest;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.Reserva;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.security.IUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CancelacionReservaValidator implements ICancelacionReservaValidator {

    public static final String MSG_SOLO_CLIENTES = "Solo los clientes pueden cancelar reservas.";
    public static final String MSG_SIN_PERMISO = "No tienes permiso para cancelar esta reserva.";
    public static final String MSG_CONFIRMACION_REQUERIDA = "Debes confirmar la cancelación de la reserva.";
    public static final String MSG_RESERVA_YA_CANCELADA = "Esta reserva ya fue cancelada.";
    public static final String MSG_HORA_INICIO_PASADA = "Una reserva cuya hora de inicio ya pasó no se puede cancelar.";
    public static final String MSG_FUERA_DE_PLAZO = "Solo puedes cancelar hasta 2 horas antes del inicio de la reserva.";
    public static final String MSG_MOTIVO_DEMASIADO_LARGO = "El motivo de la cancelación no puede superar los 250 caracteres.";

    private final Clock clock;

    @Override
    public void validarRolCliente(IUsuarioActual usuarioActual) {
        if (usuarioActual == null || !usuarioActual.esCliente()) {
            throw new AccesoDenegadoException(MSG_SOLO_CLIENTES);
        }
    }

    @Override
    public void validarPropiedadReserva(Reserva reserva, IUsuarioActual usuarioActual) {
        if (reserva == null || reserva.getClienteId() == null || usuarioActual == null
                || !reserva.getClienteId().equals(usuarioActual.getId())) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO);
        }
    }

    @Override
    public void validarConfirmacion(Boolean confirmacion) {
        if (!Boolean.TRUE.equals(confirmacion)) {
            throw new SolicitudInvalidaException(MSG_CONFIRMACION_REQUERIDA);
        }
    }

    @Override
    public void validarEstadoReserva(Reserva reserva) {
        if (reserva != null && reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new SolicitudInvalidaException(MSG_RESERVA_YA_CANCELADA);
        }
    }

    @Override
    public void validarPlazoCancelacion(Reserva reserva, LocalDateTime ahora) {
        if (reserva == null || reserva.getInicio() == null) {
            return;
        }
        LocalDateTime tiempoReferencia = ahora != null ? ahora : LocalDateTime.now(clock);

        if (tiempoReferencia.isAfter(reserva.getInicio())) {
            throw new SolicitudInvalidaException(MSG_HORA_INICIO_PASADA);
        }

        if (tiempoReferencia.isAfter(reserva.getInicio().minusHours(2))) {
            throw new SolicitudInvalidaException(MSG_FUERA_DE_PLAZO);
        }
    }

    @Override
    public void validarMotivo(String motivo) {
        if (motivo != null && motivo.length() > 250) {
            throw new SolicitudInvalidaException(MSG_MOTIVO_DEMASIADO_LARGO);
        }
    }

    @Override
    public void validarCancelacion(Reserva reserva, IUsuarioActual usuarioActual, CancelarReservaRequest request, LocalDateTime ahora) {
        validarRolCliente(usuarioActual);
        validarPropiedadReserva(reserva, usuarioActual);
        validarConfirmacion(request != null ? request.getConfirmacion() : null);
        validarEstadoReserva(reserva);
        validarPlazoCancelacion(reserva, ahora);
        validarMotivo(request != null ? request.getMotivo() : null);
    }

    @Override
    public void validarCancelacion(Reserva reserva, IUsuarioActual usuarioActual, CancelarReservaRequest request) {
        validarCancelacion(reserva, usuarioActual, request, LocalDateTime.now(clock));
    }
}
