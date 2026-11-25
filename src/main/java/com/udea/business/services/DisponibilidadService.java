package com.udea.business.services;

import com.udea.business.controllers.DTOs.FranjaDisponibleResponse;
import com.udea.business.entities.*;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.FranjaServicioRepository;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.repositories.ServicioProveedorRepository;
import com.udea.business.repositories.ServicioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DisponibilidadService implements IDisponibilidadService {

    public static final String MSG_SERVICIO_OBLIGATORIO = "El identificador del servicio es obligatorio.";
    public static final String MSG_FECHA_OBLIGATORIA = "La fecha es obligatoria para consultar franjas disponibles.";
    public static final String MSG_SERVICIO_NO_ENCONTRADO = "Servicio no encontrado.";
    public static final String MSG_SERVICIO_SIN_DISPONIBILIDAD = "Este servicio aún no tiene disponibilidad.";
    public static final String MSG_VENTANA_FECHA_INVALIDA = "Solo puedes consultar disponibilidad hasta 30 días a partir de hoy.";
    public static final String MSG_SIN_FRANJAS_DISPONIBLES = "No hay franjas disponibles para esta fecha. Intenta con otro día.";

    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ServicioRepository servicioRepository;
    private final ServicioProveedorRepository servicioProveedorRepository;
    private final FranjaServicioRepository franjaServicioRepository;
    private final ReservaRepository reservaRepository;
    private final IReservaValidator validator;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<FranjaDisponibleResponse> consultarFranjasDisponibles(Integer servicioId, LocalDate fecha, Integer proveedorId) {
        if (servicioId == null) {
            throw new SolicitudInvalidaException(MSG_SERVICIO_OBLIGATORIO);
        }

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_SERVICIO_NO_ENCONTRADO));

        // Servicio en estado "No asignado" o sin proveedores vinculados
        if ("No Asignado".equalsIgnoreCase(servicio.getEstado())
                || servicioProveedorRepository.countByServicioIdServicio(servicioId) == 0) {
            throw new SolicitudInvalidaException(MSG_SERVICIO_SIN_DISPONIBILIDAD);
        }

        if (fecha == null) {
            throw new SolicitudInvalidaException(MSG_FECHA_OBLIGATORIA);
        }

        // Solo se puede consultar disponibilidad hasta 30 días a partir de hoy
        LocalDate hoy = LocalDate.now(clock);
        LocalDate fechaLimite = hoy.plusDays(30);
        if (fecha.isBefore(hoy) || fecha.isAfter(fechaLimite)) {
            throw new SolicitudInvalidaException(MSG_VENTANA_FECHA_INVALIDA);
        }

        DiaSemana diaSemana = DiaSemana.desdeDayOfWeek(fecha.getDayOfWeek());

        // Consultar franjas definidas para el servicio y día de la semana
        List<FranjaServicio> franjasDefinidas = consultarFranjas(
                servicioId, proveedorId, diaSemana);

        LocalDateTime ahora = LocalDateTime.now(clock);
        int duracionMinutos = servicio.getDuracionMinutos();

        String moneda = (servicio.getNegocio() != null && servicio.getNegocio().getMonedaCodigoIso() != null)
                ? servicio.getNegocio().getMonedaCodigoIso()
                : "COP";

        List<FranjaDisponibleResponse> disponibles = new ArrayList<>();

        for (FranjaServicio franja : franjasDefinidas) {
            FranjaDisponibleResponse disponible = crearFranjaDisponible(
                    franja, servicio, fecha, diaSemana, duracionMinutos, moneda, ahora);
            if (disponible != null) {
                disponibles.add(disponible);
            }
        }

        // Las franjas se presentan ordenadas por hora de inicio y, ante la misma hora, por nombre del proveedor
        disponibles.sort(
                Comparator.comparing(FranjaDisponibleResponse::getHoraInicio)
                        .thenComparing(FranjaDisponibleResponse::getNombreProveedor, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
        );

        // Si no hay franjas disponibles para la fecha, el sistema lo informa con claridad
        if (disponibles.isEmpty()) {
            throw new SolicitudInvalidaException(MSG_SIN_FRANJAS_DISPONIBLES);
        }

        return disponibles;
    }

    private List<FranjaServicio> consultarFranjas(
            Integer servicioId, Integer proveedorId, DiaSemana diaSemana) {
        if (proveedorId != null) {
            return franjaServicioRepository
                    .findByServicioIdServicioAndProveedorUsuarioIdAndDiaSemana(
                            servicioId, proveedorId, diaSemana);
        }
        return franjaServicioRepository
                .findByServicioIdServicioAndDiaSemana(servicioId, diaSemana);
    }

    private FranjaDisponibleResponse crearFranjaDisponible(
            FranjaServicio franja,
            Servicio servicio,
            LocalDate fecha,
            DiaSemana diaSemana,
            int duracionMinutos,
            String moneda,
            LocalDateTime ahora) {
        LocalTime slotInicio = franja.getHoraInicio();
        LocalTime slotFin = franja.getHoraFin();
        if (slotFin == null) {
            slotFin = slotInicio.plusMinutes(duracionMinutos);
        }

        LocalDateTime slotInicioDt = LocalDateTime.of(fecha, slotInicio);
        LocalDateTime slotFinDt = LocalDateTime.of(fecha, slotFin);
        if (!slotInicioDt.isAfter(ahora)) {
            return null;
        }

        Integer idProveedor = franja.getProveedor() != null
                ? franja.getProveedor().getUsuarioId() : null;
        String nombreProveedor = obtenerNombreProveedor(franja);
        if (proveedorOcupado(idProveedor, slotInicioDt, slotFinDt)
                || !validator.hayRecursosDisponibles(servicio, slotInicioDt, slotFinDt)) {
            return null;
        }

        return FranjaDisponibleResponse.builder()
                .idFranja(franja.getIdFranja())
                .servicioId(servicio.getIdServicio())
                .nombreServicio(servicio.getNombre())
                .proveedorId(idProveedor)
                .nombreProveedor(nombreProveedor)
                .fecha(fecha.format(FECHA_FORMATTER))
                .diaSemana(diaSemana.name())
                .horaInicio(slotInicio.format(HORA_FORMATTER))
                .horaFin(slotFin.format(HORA_FORMATTER))
                .duracionMinutos(duracionMinutos)
                .precio(servicio.getPrecio())
                .moneda(moneda)
                .build();
    }

    private String obtenerNombreProveedor(FranjaServicio franja) {
        if (franja.getProveedor() == null || franja.getProveedor().getUsuario() == null) {
            return null;
        }
        return franja.getProveedor().getUsuario().getNombreCompleto();
    }

    private boolean proveedorOcupado(
            Integer idProveedor, LocalDateTime inicio, LocalDateTime fin) {
        if (idProveedor == null) {
            return false;
        }
        return !reservaRepository.findOverlappingByProveedor(
                idProveedor, inicio, fin, EstadoReserva.CONFIRMADA).isEmpty();
    }
}
