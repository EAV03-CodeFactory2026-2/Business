package com.udea.business.services;

import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.controllers.DTOs.ReservaResumenResponse;
import com.udea.business.entities.CancelacionReserva;
import com.udea.business.entities.EstadoVisibleReserva;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.PerfilProveedor;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.UsuarioPlataforma;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class ReservaConsultaMapper implements IReservaConsultaMapper {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public ReservaResumenResponse aResumen(Reserva reserva, EstadoVisibleReserva estado) {
        return ReservaResumenResponse.builder()
                .idReserva(reserva.getIdReserva())
                .codigoReserva(reserva.getCodigoReserva())
                .fecha(reserva.getInicio().format(FORMATO_FECHA))
                .horaInicio(reserva.getInicio().format(FORMATO_HORA))
                .horaFin(reserva.getFin().format(FORMATO_HORA))
                .nombreServicio(reserva.getNombreServicio())
                .nombreNegocio(nombreNegocio(reserva))
                .estado(estado.getNombre())
                .build();
    }

    @Override
    public ReservaResponse aDetalle(Reserva reserva, EstadoVisibleReserva estado) {
        Optional<Negocio> negocio = negocio(reserva);
        Optional<CancelacionReserva> cancelacion = Optional.ofNullable(reserva.getCancelacion());

        return ReservaResponse.builder()
                .idReserva(reserva.getIdReserva())
                .codigoReserva(reserva.getCodigoReserva())
                .estado(estado.getNombre())
                .servicioId(reserva.getServicioId())
                .nombreServicio(reserva.getNombreServicio())
                .negocioId(negocio.map(Negocio::getIdNegocio).orElse(null))
                .nombreNegocio(negocio.map(Negocio::getNombreNegocio).orElse(null))
                .proveedorId(reserva.getProveedorId())
                .nombreProveedor(nombreProveedor(reserva))
                .fecha(reserva.getInicio().format(FORMATO_FECHA))
                .horaInicio(reserva.getInicio().format(FORMATO_HORA))
                .horaFin(reserva.getFin().format(FORMATO_HORA))
                .duracionMinutos(reserva.getDuracionMinutos())
                .precio(reserva.getPrecioServicio())
                .moneda(reserva.getMonedaCodigoIso())
                .notas(reserva.getNotas())
                .fechaCreacion(reserva.getFechaCreacion())
                .fechaCancelacion(cancelacion.map(CancelacionReserva::getFechaCancelacion).orElse(null))
                .motivoCancelacion(cancelacion.map(CancelacionReserva::getMotivo).orElse(null))
                .build();
    }

    private Optional<Negocio> negocio(Reserva reserva) {
        return Optional.ofNullable(reserva.getServicio()).map(Servicio::getNegocio);
    }

    private String nombreNegocio(Reserva reserva) {
        return negocio(reserva).map(Negocio::getNombreNegocio).orElse(null);
    }

    private String nombreProveedor(Reserva reserva) {
        return Optional.ofNullable(reserva.getPerfilProveedor())
                .map(PerfilProveedor::getUsuario)
                .map(UsuarioPlataforma::getNombreCompleto)
                .orElse(null);
    }
}
