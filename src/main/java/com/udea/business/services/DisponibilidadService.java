package com.udea.business.services;

import com.udea.business.controllers.DTOs.FranjaDisponibleResponse;
import com.udea.business.entities.DiaSemana;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.FranjaServicio;
import com.udea.business.entities.Servicio;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.FranjaServicioRepository;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.repositories.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadService implements IDisponibilidadService {

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final String MSG_SERVICIO_OBLIGATORIO = "El identificador del servicio es obligatorio.";
    public static final String MSG_FECHA_OBLIGATORIA = "La fecha es obligatoria para consultar franjas disponibles.";
    public static final String MSG_SERVICIO_NO_ENCONTRADO = "Servicio no encontrado.";

    private final ServicioRepository servicioRepository;
    private final FranjaServicioRepository franjaServicioRepository;
    private final ReservaRepository reservaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FranjaDisponibleResponse> consultarFranjasDisponibles(Integer servicioId, LocalDate fecha, Integer proveedorId) {
        if (servicioId == null) {
            throw new SolicitudInvalidaException(MSG_SERVICIO_OBLIGATORIO);
        }
        if (fecha == null) {
            throw new SolicitudInvalidaException(MSG_FECHA_OBLIGATORIA);
        }

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_SERVICIO_NO_ENCONTRADO));

        DiaSemana diaSemana = DiaSemana.desdeDayOfWeek(fecha.getDayOfWeek());

        List<FranjaServicio> franjasDefinidas;
        if (proveedorId != null) {
            franjasDefinidas = franjaServicioRepository
                    .findByServicioIdServicioAndProveedorUsuarioIdAndDiaSemana(servicioId, proveedorId, diaSemana);
        } else {
            franjasDefinidas = franjaServicioRepository
                    .findByServicioIdServicioAndDiaSemana(servicioId, diaSemana);
        }

        List<FranjaDisponibleResponse> disponibles = new ArrayList<>();
        String moneda = (servicio.getNegocio() != null && servicio.getNegocio().getMonedaCodigoIso() != null)
                ? servicio.getNegocio().getMonedaCodigoIso()
                : "COP";

        for (FranjaServicio franja : franjasDefinidas) {
            LocalDateTime slotInicio = LocalDateTime.of(fecha, franja.getHoraInicio());
            LocalDateTime slotFin = slotInicio.plusMinutes(servicio.getDuracionMinutos());

            boolean proveedorOcupado = !reservaRepository.findOverlappingByProveedor(
                    franja.getProveedor().getUsuarioId(),
                    slotInicio,
                    slotFin,
                    EstadoReserva.CONFIRMADA
            ).isEmpty();

            if (!proveedorOcupado) {
                String nombreProveedor = (franja.getProveedor() != null && franja.getProveedor().getUsuario() != null)
                        ? franja.getProveedor().getUsuario().getNombreCompleto()
                        : null;

                disponibles.add(FranjaDisponibleResponse.builder()
                        .idFranja(franja.getIdFranja())
                        .servicioId(servicio.getIdServicio())
                        .nombreServicio(servicio.getNombre())
                        .proveedorId(franja.getProveedor() != null ? franja.getProveedor().getUsuarioId() : null)
                        .nombreProveedor(nombreProveedor)
                        .fecha(fecha.format(FECHA_FORMATTER))
                        .diaSemana(diaSemana.name())
                        .horaInicio(franja.getHoraInicio().format(HORA_FORMATTER))
                        .horaFin(franja.getHoraFin().format(HORA_FORMATTER))
                        .duracionMinutos(servicio.getDuracionMinutos())
                        .precio(servicio.getPrecio())
                        .moneda(moneda)
                        .build());
            }
        }

        return disponibles;
    }
}
