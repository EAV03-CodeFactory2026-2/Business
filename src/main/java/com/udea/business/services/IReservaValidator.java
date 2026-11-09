package com.udea.business.services;

import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.entities.FranjaServicio;
import com.udea.business.entities.Servicio;
import com.udea.business.security.IUsuarioActual;

import java.time.LocalDateTime;

public interface IReservaValidator {

    void validarRolCliente(IUsuarioActual usuarioActual);

    void validarNotas(String notas);

    void validarParametrosBasicos(CrearReservaRequest request);

    FranjaServicio obtenerYValidarFranja(CrearReservaRequest request, Servicio servicio);

    void validarDisponibilidadProveedor(Integer proveedorId, LocalDateTime inicio, LocalDateTime fin);

    void validarDisponibilidadCliente(Integer clienteId, LocalDateTime inicio, LocalDateTime fin);

    void validarDisponibilidadRecursos(Servicio servicio, LocalDateTime inicio, LocalDateTime fin);

    boolean hayRecursosDisponibles(Servicio servicio, LocalDateTime inicio, LocalDateTime fin);
}
