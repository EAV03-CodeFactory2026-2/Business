package com.udea.business.services;

import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.entities.DiaSemana;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.FranjaServicio;
import com.udea.business.entities.RecursoNegocio;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.ServicioRecurso;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.FranjaServicioRepository;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.repositories.ServicioRecursoRepository;
import com.udea.business.security.IUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ReservaValidator implements IReservaValidator {

    public static final String MSG_SOLO_CLIENTES = "Solo los clientes pueden realizar reservas.";
    public static final String MSG_NOTAS_LARGAS = "Las notas no pueden superar los 250 caracteres.";
    public static final String MSG_FRANJA_RESERVADA = "Esta franja acaba de ser reservada. Elige otra.";
    public static final String MSG_CRUCE_CLIENTE = "Ya tienes una reserva en ese horario.";
    public static final String MSG_HORARIO_NO_DISPONIBLE = "Este horario no está disponible para reserva.";
    public static final String MSG_FRANJA_NO_DISPONIBLE = "Esta franja ya no está disponible. Elige otra.";
    public static final String MSG_SOLICITUD_VACIA = "La solicitud de reserva no puede estar vacía.";
    public static final String MSG_FECHA_OBLIGATORIA = "La fecha de reserva es obligatoria.";
    public static final String MSG_HORA_OBLIGATORIA = "La hora de inicio es obligatoria.";
    public static final String MSG_SERVICIO_OBLIGATORIO = "El identificador del servicio es obligatorio.";
    public static final String MSG_PROVEEDOR_OBLIGATORIO = "El identificador del proveedor es obligatorio.";

    private final FranjaServicioRepository franjaServicioRepository;
    private final ReservaRepository reservaRepository;
    private final ServicioRecursoRepository servicioRecursoRepository;

    @Override
    public void validarRolCliente(IUsuarioActual usuarioActual) {
        if (usuarioActual == null || !usuarioActual.esCliente()) {
            throw new AccesoDenegadoException(MSG_SOLO_CLIENTES);
        }
    }

    @Override
    public void validarNotas(String notas) {
        if (notas != null && notas.length() > 250) {
            throw new SolicitudInvalidaException(MSG_NOTAS_LARGAS);
        }
    }

    @Override
    public void validarParametrosBasicos(CrearReservaRequest request) {
        if (request == null) {
            throw new SolicitudInvalidaException(MSG_SOLICITUD_VACIA);
        }
        if (request.getFecha() == null) {
            throw new SolicitudInvalidaException(MSG_FECHA_OBLIGATORIA);
        }
        if (request.getFranjaId() == null) {
            if (request.getServicioId() == null) {
                throw new SolicitudInvalidaException(MSG_SERVICIO_OBLIGATORIO);
            }
            if (request.getProveedorId() == null) {
                throw new SolicitudInvalidaException(MSG_PROVEEDOR_OBLIGATORIO);
            }
            if (request.getHoraInicio() == null) {
                throw new SolicitudInvalidaException(MSG_HORA_OBLIGATORIA);
            }
        }
    }

    @Override
    public FranjaServicio obtenerYValidarFranja(CrearReservaRequest request, Servicio servicio) {
        DiaSemana diaSemana = DiaSemana.desdeDayOfWeek(request.getFecha().getDayOfWeek());

        if (request.getFranjaId() != null) {
            // El cliente seleccionó una franja que fue eliminada mientras decidía
            FranjaServicio franja = franjaServicioRepository.findById(request.getFranjaId())
                    .orElseThrow(() -> new SolicitudInvalidaException(MSG_FRANJA_NO_DISPONIBLE));

            if (servicio != null && !franja.getServicio().getIdServicio().equals(servicio.getIdServicio())) {
                throw new SolicitudInvalidaException(MSG_FRANJA_NO_DISPONIBLE);
            }
            if (request.getProveedorId() != null && !franja.getProveedor().getUsuarioId().equals(request.getProveedorId())) {
                throw new SolicitudInvalidaException(MSG_FRANJA_NO_DISPONIBLE);
            }
            if (request.getHoraInicio() != null && !franja.getHoraInicio().equals(request.getHoraInicio())) {
                throw new SolicitudInvalidaException(MSG_FRANJA_NO_DISPONIBLE);
            }
            if (franja.getDiaSemana() != diaSemana) {
                throw new SolicitudInvalidaException(MSG_FRANJA_NO_DISPONIBLE);
            }
            return franja;
        }

        // No se pasó franjaId explícito. Buscar si existe una franja definida para ese horario
        Integer servicioId = request.getServicioId();
        Integer proveedorId = request.getProveedorId();
        LocalTime horaInicio = request.getHoraInicio();

        return franjaServicioRepository
                .findByServicioIdServicioAndProveedorUsuarioIdAndDiaSemanaAndHoraInicio(
                        servicioId, proveedorId, diaSemana, horaInicio)
                .orElseThrow(() -> new SolicitudInvalidaException(MSG_HORARIO_NO_DISPONIBLE));
    }

    @Override
    public void validarDisponibilidadProveedor(Integer proveedorId, LocalDateTime inicio, LocalDateTime fin) {
        // Dos reservas confirmadas no pueden ocupar al mismo proveedor al mismo tiempo
        boolean ocupado = !reservaRepository.findOverlappingByProveedor(
                proveedorId, inicio, fin, EstadoReserva.CONFIRMADA).isEmpty();
        if (ocupado) {
            throw new RecursoDuplicadoException(MSG_FRANJA_RESERVADA);
        }
    }

    @Override
    public void validarDisponibilidadCliente(Integer clienteId, LocalDateTime inicio, LocalDateTime fin) {
        // Un cliente no puede tener dos reservas confirmadas que se crucen en el tiempo
        boolean cruceCliente = !reservaRepository.findOverlappingByCliente(
                clienteId, inicio, fin, EstadoReserva.CONFIRMADA).isEmpty();
        if (cruceCliente) {
            throw new SolicitudInvalidaException(MSG_CRUCE_CLIENTE);
        }
    }

    @Override
    public void validarDisponibilidadRecursos(Servicio servicio, LocalDateTime inicio, LocalDateTime fin) {
        if (servicio == null || servicio.getIdServicio() == null || inicio == null || fin == null) {
            return;
        }

        List<ServicioRecurso> recursosRequeridos = servicioRecursoRepository.findByServicioIdWithRecurso(servicio.getIdServicio());
        if (recursosRequeridos.isEmpty()) {
            return;
        }

        Integer negocioId = (servicio.getNegocio() != null && servicio.getNegocio().getIdNegocio() != null)
                ? servicio.getNegocio().getIdNegocio()
                : recursosRequeridos.get(0).getNegocioId();

        List<Reserva> solapadas = negocioId != null
                ? reservaRepository.findOverlappingConfirmedByNegocio(negocioId, inicio, fin, EstadoReserva.CONFIRMADA)
                : reservaRepository.findOverlappingConfirmed(inicio, fin, EstadoReserva.CONFIRMADA);
        if (solapadas.isEmpty()) {
            for (ServicioRecurso sr : recursosRequeridos) {
                RecursoNegocio recurso = sr.getRecursoNegocio();
                int capacidadTotal = (recurso != null && recurso.getCantidad() != null) ? recurso.getCantidad() : 0;
                int requerida = sr.getCantidadRequerida() != null ? sr.getCantidadRequerida() : 0;
                if (requerida > capacidadTotal) {
                    String nombre = recurso != null ? recurso.getNombre() : "requerido";
                    throw new SolicitudInvalidaException(
                            String.format("No hay unidades suficientes del recurso %s para esa franja.", nombre)
                    );
                }
            }
            return;
        }

        Map<Integer, List<ServicioRecurso>> recursosPorServicio = new HashMap<>();

        for (ServicioRecurso sr : recursosRequeridos) {
            RecursoNegocio recurso = sr.getRecursoNegocio();
            Integer recursoId = recurso != null ? recurso.getIdRecurso() : null;
            int capacidadTotal = (recurso != null && recurso.getCantidad() != null) ? recurso.getCantidad() : 0;
            int requerida = sr.getCantidadRequerida() != null ? sr.getCantidadRequerida() : 0;

            int ocupado = 0;
            for (Reserva solapada : solapadas) {
                Integer servicioSolapadoId = solapada.getServicioId();
                List<ServicioRecurso> recursosDeSolapado = recursosPorServicio.computeIfAbsent(
                        servicioSolapadoId,
                        servicioRecursoRepository::findByServicioIdWithRecurso
                );
                for (ServicioRecurso srSolapado : recursosDeSolapado) {
                    if (srSolapado.getRecursoNegocio() != null
                            && Objects.equals(srSolapado.getRecursoNegocio().getIdRecurso(), recursoId)) {
                        ocupado += (srSolapado.getCantidadRequerida() != null ? srSolapado.getCantidadRequerida() : 0);
                    }
                }
            }

            if (ocupado + requerida > capacidadTotal) {
                String nombre = recurso != null ? recurso.getNombre() : "requerido";
                throw new SolicitudInvalidaException(
                        String.format("No hay unidades suficientes del recurso %s para esa franja.", nombre)
                );
            }
        }
    }

    @Override
    public boolean hayRecursosDisponibles(Servicio servicio, LocalDateTime inicio, LocalDateTime fin) {
        try {
            validarDisponibilidadRecursos(servicio, inicio, fin);
            return true;
        } catch (SolicitudInvalidaException e) {
            return false;
        }
    }
}
