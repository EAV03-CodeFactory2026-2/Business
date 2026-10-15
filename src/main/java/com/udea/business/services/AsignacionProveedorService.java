package com.udea.business.services;

import com.udea.business.controllers.DTOs.AsignacionProveedoresResponse;
import com.udea.business.controllers.DTOs.ProveedorAsignadoResponse;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionProveedorService implements IAsignacionProveedorService {

    public static final String MSG_SOLO_PROPIETARIO = "Solo un Propietario puede asignar proveedores a los servicios.";
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
        Servicio servicio = validarAccesoAlServicio(servicioId);
        Set<Integer> ids = validarSeleccion(proveedorIds);
        List<PerfilProveedor> proveedores = buscarProveedoresValidos(ids);
        validarQueNoEstenVinculados(servicioId, proveedores);

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

    private Servicio validarAccesoAlServicio(Integer servicioId) {
        if (!usuarioActual.esPropietario()) {
            throw new AccesoDenegadoException(MSG_SOLO_PROPIETARIO);
        }
        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_SERVICIO_NO_ENCONTRADO));
        if (!servicio.getNegocio().getPropietarioId().equals(usuarioActual.getId())) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO);
        }
        return servicio;
    }

    private Set<Integer> validarSeleccion(List<Integer> proveedorIds) {
        if (proveedorIds == null || proveedorIds.isEmpty()) {
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
        return ids;
    }

    /** Devuelve los proveedores en el mismo orden en que se pidieron. */
    private List<PerfilProveedor> buscarProveedoresValidos(Set<Integer> ids) {
        Map<Integer, PerfilProveedor> encontrados = perfilProveedorRepository
                .buscarActivosPorIds(ids, RolPlataforma.PROVEEDOR).stream()
                .collect(Collectors.toMap(PerfilProveedor::getUsuarioId, Function.identity()));

        List<Integer> invalidos = ids.stream().filter(id -> !encontrados.containsKey(id)).toList();
        if (!invalidos.isEmpty()) {
            throw new SolicitudInvalidaException(String.format(MSG_NO_SON_PROVEEDORES, unir(invalidos)));
        }
        return ids.stream().map(encontrados::get).toList();
    }

    private void validarQueNoEstenVinculados(Integer servicioId, List<PerfilProveedor> proveedores) {
        Set<Integer> yaVinculados = new HashSet<>(servicioProveedorRepository.buscarProveedoresYaVinculados(
                servicioId, proveedores.stream().map(PerfilProveedor::getUsuarioId).toList()));
        if (yaVinculados.isEmpty()) {
            return;
        }
        String detalle = proveedores.stream()
                .filter(p -> yaVinculados.contains(p.getUsuarioId()))
                .map(PerfilProveedor::getIdentificacion)
                .collect(Collectors.joining(", "));
        throw new RecursoDuplicadoException(String.format(MSG_YA_VINCULADOS, detalle));
    }

    private String unir(List<Integer> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }
}
