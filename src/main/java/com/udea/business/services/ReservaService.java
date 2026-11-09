package com.udea.business.services;

import com.udea.business.controllers.DTOs.CrearReservaRequest;
import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.entities.*;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.*;
import com.udea.business.security.IUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService implements IReservaService {

    public static final String MSG_RESERVA_CONFIRMADA = "Reserva confirmada exitosamente.";
    public static final String MSG_RESERVA_NO_ENCONTRADA = "Reserva no encontrada.";
    public static final String MSG_SERVICIO_NO_ENCONTRADO = "Servicio no encontrado.";
    public static final String MSG_FRANJA_RESERVADA = "Esta franja acaba de ser reservada. Elige otra.";

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA_ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FORMATO_FECHA_ES = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IReservaValidator validator;
    private final ICodigoReservaGenerator codigoReservaGenerator;
    private final IUsuarioActual usuarioActual;
    private final ReservaRepository reservaRepository;
    private final ServicioRepository servicioRepository;
    private final PerfilProveedorRepository perfilProveedorRepository;
    private final UsuarioPlataformaRepository usuarioPlataformaRepository;
    private final FranjaServicioRepository franjaServicioRepository;

    @Override
    @Transactional
    public ReservaResponse crearReserva(CrearReservaRequest request) {
        validator.validarParametrosBasicos(request);
        validator.validarRolCliente(usuarioActual);
        validator.validarNotas(request.getNotas());

        Integer clienteId = usuarioActual.getId();

        Integer servicioId = request.getServicioId();
        if (servicioId == null && request.getFranjaId() != null) {
            FranjaServicio franjaReferencia = franjaServicioRepository.findById(request.getFranjaId())
                    .orElseThrow(() -> new SolicitudInvalidaException(ReservaValidator.MSG_FRANJA_NO_DISPONIBLE));
            servicioId = franjaReferencia.getServicio().getIdServicio();
        }

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_SERVICIO_NO_ENCONTRADO));

        FranjaServicio franja = validator.obtenerYValidarFranja(request, servicio);

        Integer proveedorId = franja.getProveedor().getUsuarioId();
        LocalTime horaInicio = franja.getHoraInicio();
        Integer duracionMinutos = servicio.getDuracionMinutos();

        LocalDateTime inicio = LocalDateTime.of(request.getFecha(), horaInicio);
        LocalDateTime fin = inicio.plusMinutes(duracionMinutos);

        // Bloqueo pesimista del proveedor para prevenir colisiones concurrentes (CA-03)
        perfilProveedorRepository.findByIdConBloqueo(proveedorId);

        // Validar que no haya cruces para el proveedor ni para el cliente
        validator.validarDisponibilidadProveedor(proveedorId, inicio, fin);
        validator.validarDisponibilidadCliente(clienteId, inicio, fin);

        String nombreServicio = servicio.getNombre();
        BigDecimal precioServicio = servicio.getPrecio();
        String monedaCodigoIso = (servicio.getNegocio() != null && servicio.getNegocio().getMonedaCodigoIso() != null)
                ? servicio.getNegocio().getMonedaCodigoIso()
                : "COP";

        String codigoReserva = codigoReservaGenerator.generarCodigo();
        String notasLimpia = (request.getNotas() != null && !request.getNotas().trim().isEmpty())
                ? request.getNotas().trim()
                : null;

        Reserva nuevaReserva = Reserva.builder()
                .codigoReserva(codigoReserva)
                .clienteId(clienteId)
                .proveedorId(proveedorId)
                .servicioId(servicio.getIdServicio())
                .inicio(inicio)
                .fin(fin)
                .notas(notasLimpia)
                .nombreServicio(nombreServicio)
                .duracionMinutos(duracionMinutos)
                .precioServicio(precioServicio)
                .monedaCodigoIso(monedaCodigoIso)
                .estado(EstadoReserva.CONFIRMADA)
                .fechaCreacion(OffsetDateTime.now())
                .fechaActualizacion(OffsetDateTime.now())
                .build();

        Reserva guardada;
        try {
            guardada = reservaRepository.saveAndFlush(nuevaReserva);
        } catch (DataIntegrityViolationException e) {
            throw new RecursoDuplicadoException(MSG_FRANJA_RESERVADA);
        }

        return construirRespuesta(guardada, servicio, franja.getProveedor(), clienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse consultarPorId(Integer idReserva) {
        if (idReserva == null) {
            throw new SolicitudInvalidaException("El identificador de la reserva es obligatorio.");
        }
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_RESERVA_NO_ENCONTRADA));
        return mapearDesdeEntidad(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse consultarPorCodigo(String codigoReserva) {
        if (codigoReserva == null || codigoReserva.trim().isEmpty()) {
            throw new SolicitudInvalidaException("El código de la reserva es obligatorio.");
        }
        Reserva reserva = reservaRepository.findByCodigoReserva(codigoReserva.trim())
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_RESERVA_NO_ENCONTRADA));
        return mapearDesdeEntidad(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> consultarMisReservas() {
        validator.validarRolCliente(usuarioActual);
        Integer clienteId = usuarioActual.getId();
        return reservaRepository.findByClienteIdOrderByInicioDesc(clienteId).stream()
                .map(this::mapearDesdeEntidad)
                .toList();
    }

    private ReservaResponse construirRespuesta(Reserva reserva, Servicio servicio, PerfilProveedor proveedor, Integer clienteId) {
        String nombreNegocio = (servicio != null && servicio.getNegocio() != null)
                ? servicio.getNegocio().getNombreNegocio()
                : null;
        Integer negocioId = (servicio != null && servicio.getNegocio() != null)
                ? servicio.getNegocio().getIdNegocio()
                : null;

        String nombreProveedor = (proveedor != null && proveedor.getUsuario() != null)
                ? proveedor.getUsuario().getNombreCompleto()
                : null;

        String nombreCliente = usuarioPlataformaRepository.findById(clienteId)
                .map(UsuarioPlataforma::getNombreCompleto)
                .orElse(null);

        String resumen = generarResumen(
                reserva.getNombreServicio(),
                nombreNegocio,
                nombreProveedor,
                reserva.getInicio(),
                reserva.getFin()
        );

        return ReservaResponse.builder()
                .mensaje(MSG_RESERVA_CONFIRMADA)
                .idReserva(reserva.getIdReserva())
                .codigoReserva(reserva.getCodigoReserva())
                .estado(reserva.getEstado() != null ? reserva.getEstado().getNombre() : EstadoReserva.CONFIRMADA.getNombre())
                .servicioId(reserva.getServicioId())
                .nombreServicio(reserva.getNombreServicio())
                .negocioId(negocioId)
                .nombreNegocio(nombreNegocio)
                .proveedorId(reserva.getProveedorId())
                .nombreProveedor(nombreProveedor)
                .clienteId(reserva.getClienteId())
                .nombreCliente(nombreCliente)
                .fecha(reserva.getInicio().format(FORMATO_FECHA_ISO))
                .horaInicio(reserva.getInicio().format(FORMATO_HORA))
                .horaFin(reserva.getFin().format(FORMATO_HORA))
                .duracionMinutos(reserva.getDuracionMinutos())
                .precio(reserva.getPrecioServicio())
                .moneda(reserva.getMonedaCodigoIso())
                .notas(reserva.getNotas())
                .resumen(resumen)
                .fechaCreacion(reserva.getFechaCreacion())
                .build();
    }

    private ReservaResponse mapearDesdeEntidad(Reserva reserva) {
        Servicio servicio = servicioRepository.findById(reserva.getServicioId()).orElse(null);
        PerfilProveedor proveedor = perfilProveedorRepository.findById(reserva.getProveedorId()).orElse(null);
        return construirRespuesta(reserva, servicio, proveedor, reserva.getClienteId());
    }

    private String generarResumen(String nombreServicio, String nombreNegocio, String nombreProveedor, LocalDateTime inicio, LocalDateTime fin) {
        String diaSemana = DiaSemana.desdeDayOfWeek(inicio.getDayOfWeek()).getNombre();
        String fechaStr = inicio.format(FORMATO_FECHA_ES);
        String horaInicioStr = inicio.format(FORMATO_HORA);
        String horaFinStr = fin.format(FORMATO_HORA);

        return String.format("\"%s\", \"%s\", \"%s\", %s %s, %s-%s",
                nombreServicio != null ? nombreServicio : "",
                nombreNegocio != null ? nombreNegocio : "",
                nombreProveedor != null ? nombreProveedor : "",
                diaSemana,
                fechaStr,
                horaInicioStr,
                horaFinStr);
    }
}
