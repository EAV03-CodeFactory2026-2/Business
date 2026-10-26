package com.udea.business.services;

import com.udea.business.controllers.DTOs.AsignacionProveedoresResponse;
import com.udea.business.controllers.DTOs.ProveedorAsignadoResponse;
import com.udea.business.controllers.DTOs.ServicioAsignadoResponse;
import com.udea.business.entities.EstadoServicio;
import com.udea.business.entities.PerfilProveedor;
import com.udea.business.entities.RolPlataforma;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.ServicioProveedor;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.PerfilProveedorRepository;
import com.udea.business.repositories.ServicioProveedorRepository;
import com.udea.business.repositories.ServicioRepository;
import com.udea.business.security.IUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionProveedorService implements IAsignacionProveedorService {

    public static final String MSG_SOLO_PROPIETARIO = "Solo un Propietario puede asignar proveedores a los servicios.";
    public static final String MSG_SOLO_PROVEEDOR = "Solo un Proveedor puede consultar sus servicios asignados.";
    public static final String MSG_SIN_PERMISO = "No tienes permiso para asignar proveedores a este servicio.";
    public static final String MSG_SERVICIO_NO_ENCONTRADO = "Servicio no encontrado.";
    public static final String MSG_SIN_SELECCION = "Selecciona al menos un proveedor para asignar.";
    public static final String MSG_ID_INVALIDO = "La selección contiene un proveedor sin identificador.";
    public static final String MSG_REPETIDOS = "No repitas el mismo proveedor en la solicitud.";
    public static final String MSG_NO_SON_PROVEEDORES = "Estos usuarios no son proveedores activos de la plataforma: %s.";
    public static final String MSG_YA_VINCULADOS = "Ya está vinculado a este servicio: %s.";
    public static final String MSG_ASIGNADO = "Servicio asignado a %d proveedor(es)";

    private final ServicioRepository servicioRepository;
    private final PerfilProveedorRepository perfilProveedorRepository;
    private final ServicioProveedorRepository servicioProveedorRepository;
    private final IUsuarioActual usuarioActual;

    /**
     * Vincula el servicio con todos los proveedores indicados o con ninguno:
     * si alguno no es válido o ya estaba vinculado, no se guarda nada.
     */
    @Override
    @Transactional
    public AsignacionProveedoresResponse asignar(Integer servicioId, List<Integer> proveedorIds) {
        Servicio servicio = obtenerServicioPropio(servicioId);

        List<PerfilProveedor> proveedores = resolverProveedores(servicioId, proveedorIds);
        List<ServicioProveedor> vinculaciones = proveedores.stream()
                .map(p -> ServicioProveedor.builder().servicio(servicio).proveedor(p).build())
                .toList();
        try {
            servicioProveedorRepository.saveAllAndFlush(vinculaciones);
        } catch (DataIntegrityViolationException e) {
            // Otra petición vinculó el mismo proveedor al mismo tiempo; la restricción única lo frena.
            throw new RecursoDuplicadoException(
                    String.format(MSG_YA_VINCULADOS, "uno de los proveedores seleccionados"));
        }

        long totalProveedores = servicioProveedorRepository.countByServicioIdServicio(servicioId);
        servicio.setEstado(EstadoServicio.segunProveedores(totalProveedores));
        servicioRepository.save(servicio);

        return AsignacionProveedoresResponse.builder()
                .mensaje(String.format(MSG_ASIGNADO, vinculaciones.size()))
                .idServicio(servicioId)
                .estadoServicio(servicio.getEstado())
                .proveedoresAsignados(proveedores.stream()
                        .map(ProveedorAsignadoResponse::desde)
                        .toList())
                .totalProveedores(totalProveedores)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorAsignadoResponse> consultarProveedoresAsignados(Integer servicioId) {
        obtenerServicioPropio(servicioId);
        return servicioProveedorRepository.findByServicioIdConDetalle(servicioId).stream()
                .map(sp -> ProveedorAsignadoResponse.desde(sp.getProveedor()))
                .toList();
    }

    private Servicio obtenerServicioPropio(Integer servicioId) {
        if (!usuarioActual.esPropietario()) {
            throw new AccesoDenegadoException(MSG_SOLO_PROPIETARIO);
        }
        Servicio servicio = servicioRepository.findById(servicioId).orElse(null);
        if (servicio == null) {
            throw new RecursoNoEncontradoException(MSG_SERVICIO_NO_ENCONTRADO);
        }
        if (!servicio.getNegocio().getPropietarioId().equals(usuarioActual.getId())) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO);
        }
        return servicio;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioAsignadoResponse> consultarMisServicios() {
        if (!usuarioActual.esProveedor()) {
            throw new AccesoDenegadoException(MSG_SOLO_PROVEEDOR);
        }
        Integer proveedorId = usuarioActual.getId();
        List<ServicioProveedor> asignaciones = servicioProveedorRepository.findByProveedorIdConServicioYNegocio(proveedorId);
        return asignaciones.stream()
                .map(sp -> ServicioAsignadoResponse.desde(sp.getServicio()))
                .toList();
    }

    /** Valida la selección y devuelve los proveedores en el mismo orden en que se pidieron. */
    private List<PerfilProveedor> resolverProveedores(Integer servicioId, List<Integer> proveedorIds) {
        if (CollectionUtils.isEmpty(proveedorIds)) {
            throw new SolicitudInvalidaException(MSG_SIN_SELECCION);
        }
        // contains(null) lanza NullPointerException en listas inmutables (List.of), por eso se usa el stream.
        if (proveedorIds.stream().anyMatch(Objects::isNull)) {
            throw new SolicitudInvalidaException(MSG_ID_INVALIDO);
        }
        Set<Integer> ids = new LinkedHashSet<>(proveedorIds);
        if (ids.size() != proveedorIds.size()) {
            throw new SolicitudInvalidaException(MSG_REPETIDOS);
        }

        Map<Integer, PerfilProveedor> encontrados = perfilProveedorRepository
                .buscarActivosPorIds(ids, RolPlataforma.PROVEEDOR).stream()
                .collect(Collectors.toMap(PerfilProveedor::getUsuarioId, Function.identity()));

        String invalidos = ids.stream()
                .filter(Predicate.not(encontrados::containsKey))
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        if (!invalidos.isEmpty()) {
            throw new SolicitudInvalidaException(String.format(MSG_NO_SON_PROVEEDORES, invalidos));
        }

        List<Integer> yaVinculados = servicioProveedorRepository.buscarProveedoresYaVinculados(
                servicioId, List.copyOf(ids));
        if (!yaVinculados.isEmpty()) {
            String detalle = yaVinculados.stream()
                    .map(encontrados::get)
                    .map(PerfilProveedor::getIdentificacion)
                    .collect(Collectors.joining(", "));
            throw new RecursoDuplicadoException(String.format(MSG_YA_VINCULADOS, detalle));
        }

        return ids.stream().map(encontrados::get).toList();
    }
}
