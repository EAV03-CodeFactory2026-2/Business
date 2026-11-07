package com.udea.business.services;

import com.udea.business.controllers.DTOs.AsociacionRecursoResponse;
import com.udea.business.controllers.DTOs.AsociarRecursoRequest;
import com.udea.business.controllers.DTOs.RecursoRequeridoResponse;
import com.udea.business.entities.RecursoNegocio;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.ServicioRecurso;
import com.udea.business.entities.ServicioRecurso.ServicioRecursoId;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.RecursoNegocioRepository;
import com.udea.business.repositories.ServicioRecursoRepository;
import com.udea.business.repositories.ServicioRepository;
import com.udea.business.security.IUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServicioRecursoService implements IServicioRecursoService {

    public static final String MSG_SOLO_PROPIETARIO = "Solo un Propietario puede gestionar recursos de los servicios.";
    public static final String MSG_SIN_PERMISO = "No tienes permiso para modificar este servicio.";
    public static final String MSG_SERVICIO_NO_ENCONTRADO = "Servicio no encontrado.";
    public static final String MSG_RECURSO_NO_ENCONTRADO = "Recurso no encontrado.";
    public static final String MSG_RECURSO_YA_ASOCIADO = "Este recurso ya está asociado al servicio.";
    public static final String MSG_RECURSO_OTRO_NEGOCIO = "Solo puedes asociar recursos del mismo negocio del servicio.";
    public static final String MSG_RECURSO_ASOCIADO = "Recurso asociado al servicio.";
    public static final String MSG_CANTIDAD_INVALIDA = "La cantidad requerida debe ser al menos 1.";
    public static final String MSG_RECURSO_ID_OBLIGATORIO = "El identificador del recurso es obligatorio.";

    private final ServicioRepository servicioRepository;
    private final RecursoNegocioRepository recursoNegocioRepository;
    private final ServicioRecursoRepository servicioRecursoRepository;
    private final IUsuarioActual usuarioActual;

    @Override
    @Transactional
    public AsociacionRecursoResponse asociarRecurso(Integer servicioId, AsociarRecursoRequest request) {
        Servicio servicio = obtenerServicioPropio(servicioId);

        validar(request == null || request.getRecursoId() == null,
                new SolicitudInvalidaException(MSG_RECURSO_ID_OBLIGATORIO));
        validar(request.getCantidadRequerida() == null || request.getCantidadRequerida() < 1,
                new SolicitudInvalidaException(MSG_CANTIDAD_INVALIDA));

        Integer recursoId = request.getRecursoId();
        Short cantidadRequerida = request.getCantidadRequerida();

        // CA-05: El recurso no debe estar previamente asociado al servicio
        ServicioRecursoId pk = new ServicioRecursoId(servicioId, recursoId);
        validar(servicioRecursoRepository.existsById(pk),
                new RecursoDuplicadoException(MSG_RECURSO_YA_ASOCIADO));

        // CA-04: El recurso debe existir y pertenecer al mismo negocio del servicio
        RecursoNegocio recurso = recursoNegocioRepository.findById(recursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_RECURSO_NO_ENCONTRADO));

        Integer negocioId = servicio.getNegocio().getIdNegocio();
        validar(!recurso.getNegocioId().equals(negocioId),
                new SolicitudInvalidaException(MSG_RECURSO_OTRO_NEGOCIO));

        // CA-03: La cantidad requerida no puede ser mayor a las unidades disponibles del recurso
        validar(cantidadRequerida > recurso.getCantidad(),
                new SolicitudInvalidaException(String.format("El recurso %s solo tiene %d unidades.",
                        recurso.getNombre(), recurso.getCantidad())));

        ServicioRecurso asociacion = ServicioRecurso.builder()
                .id(pk)
                .servicio(servicio)
                .recursoNegocio(recurso)
                .negocioId(negocioId)
                .cantidadRequerida(cantidadRequerida)
                .build();

        servicioRecursoRepository.save(asociacion);

        List<RecursoRequeridoResponse> listaActualizada = obtenerListaRecursosRequeridos(servicioId);

        return AsociacionRecursoResponse.builder()
                .mensaje(MSG_RECURSO_ASOCIADO)
                .idServicio(servicioId)
                .recursosRequeridos(listaActualizada)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecursoRequeridoResponse> consultarRecursosRequeridos(Integer servicioId) {
        obtenerServicioPropio(servicioId);
        return obtenerListaRecursosRequeridos(servicioId);
    }

    @Override
    @Transactional
    public void quitarRecurso(Integer servicioId, Integer recursoId) {
        obtenerServicioPropio(servicioId);

        validar(recursoId == null, new SolicitudInvalidaException(MSG_RECURSO_ID_OBLIGATORIO));

        ServicioRecursoId pk = new ServicioRecursoId(servicioId, recursoId);
        validar(!servicioRecursoRepository.existsById(pk),
                new RecursoNoEncontradoException(MSG_RECURSO_NO_ENCONTRADO));

        servicioRecursoRepository.deleteById(pk);
    }

    private Servicio obtenerServicioPropio(Integer servicioId) {
        validar(!usuarioActual.esPropietario(), new AccesoDenegadoException(MSG_SOLO_PROPIETARIO));

        validar(servicioId == null, new SolicitudInvalidaException("El identificador del servicio es obligatorio."));

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_SERVICIO_NO_ENCONTRADO));

        validar(!servicio.getNegocio().getPropietarioId().equals(usuarioActual.getId()),
                new AccesoDenegadoException(MSG_SIN_PERMISO));

        return servicio;
    }

    private List<RecursoRequeridoResponse> obtenerListaRecursosRequeridos(Integer servicioId) {
        return servicioRecursoRepository.findByServicioIdWithRecurso(servicioId).stream()
                .map(sr -> RecursoRequeridoResponse.builder()
                        .idRecurso(sr.getRecursoNegocio().getIdRecurso())
                        .nombreRecurso(sr.getRecursoNegocio().getNombre())
                        .cantidadRequerida(sr.getCantidadRequerida())
                        .build())
                .toList();
    }

    private void validar(boolean condicion, RuntimeException excepcion) {
        if (condicion) {
            throw excepcion;
        }
    }
}
