package com.udea.business.services;

import com.udea.business.controllers.DTOs.RecursoCreateRequest;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.Recurso;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.repositories.NegocioRepository;
import com.udea.business.repositories.RecursoRepository;
import com.udea.business.util.TextoNormalizador;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecursoService implements IRecursoService {

    public static final String MSG_SOLO_PROPIETARIO = "Solo un Propietario puede gestionar los recursos del negocio.";
    public static final String MSG_SIN_PERMISO_NEGOCIO = "No tienes permiso para gestionar los recursos de este negocio.";
    public static final String MSG_NEGOCIO_NO_ENCONTRADO = "Negocio no encontrado.";
    public static final String MSG_NOMBRE_DUPLICADO = "Ya existe un recurso con este nombre en el negocio.";
    public static final String MSG_CANTIDAD_INVALIDA = "La cantidad debe ser un número entero entre 1 y 999.";
    public static final String MSG_NOMBRE_OBLIGATORIO = "El nombre del recurso es obligatorio.";
    public static final String MSG_NOMBRE_LARGO = "El nombre del recurso no puede exceder los 100 caracteres.";
    public static final String MSG_TIPO_INVALIDO = "El tipo del recurso debe ser Espacio o Equipo.";
    public static final String MSG_DESCRIPCION_LARGA = "La descripción no puede exceder los 500 caracteres.";

    private static final BigInteger CANTIDAD_MINIMA = BigInteger.ONE;
    private static final BigInteger CANTIDAD_MAXIMA = BigInteger.valueOf(999);
    private static final int NOMBRE_MAX = 100;
    private static final int DESCRIPCION_MAX = 500;
    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "espacio", "Espacio",
            "equipo", "Equipo"
    );

    private final RecursoRepository recursoRepository;
    private final NegocioRepository negocioRepository;

    @Override
    @Transactional
    public Recurso registrarRecurso(Integer negocioId, RecursoCreateRequest request, Integer usuarioIdAutenticado) {
        return registrarRecurso(negocioId, request, usuarioIdAutenticado, tieneRolPropietarioEnContexto());
    }

    @Override
    @Transactional
    public Recurso registrarRecurso(Integer negocioId, RecursoCreateRequest request, Integer usuarioIdAutenticado, boolean esPropietario) {
        // Los permisos se validan antes que los datos: sin permiso siempre es 403.
        Negocio negocio = validarAccesoANegocio(negocioId, usuarioIdAutenticado, esPropietario);

        String nombre = validarNombre(request.getNombre());
        String tipo = validarTipo(request.getTipo());
        Integer cantidad = validarCantidad(request.getCantidad());
        String descripcion = validarDescripcion(request.getDescripcion());
        String nombreNormalizado = TextoNormalizador.normalizar(nombre);

        if (recursoRepository.existsByNegocioIdNegocioAndNombreNormalizado(negocioId, nombreNormalizado)) {
            throw new RecursoDuplicadoException(MSG_NOMBRE_DUPLICADO);
        }

        Recurso recurso = Recurso.builder()
                .negocio(negocio)
                .nombre(nombre)
                .nombreNormalizado(nombreNormalizado)
                .tipo(tipo)
                .cantidad(cantidad)
                .descripcion(descripcion)
                .estado("Activo")
                .build();

        try {
            return recursoRepository.saveAndFlush(recurso);
        } catch (DataIntegrityViolationException e) {
            // Dos registros concurrentes con el mismo nombre: los frena la restricción única de la BD.
            throw new RecursoDuplicadoException(MSG_NOMBRE_DUPLICADO);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recurso> listarRecursos(Integer negocioId, Integer usuarioIdAutenticado) {
        return listarRecursos(negocioId, usuarioIdAutenticado, tieneRolPropietarioEnContexto());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Recurso> listarRecursos(Integer negocioId, Integer usuarioIdAutenticado, boolean esPropietario) {
        validarAccesoANegocio(negocioId, usuarioIdAutenticado, esPropietario);
        return recursoRepository.findByNegocioIdNegocioOrderByNombreAsc(negocioId);
    }

    private Negocio validarAccesoANegocio(Integer negocioId, Integer usuarioIdAutenticado, boolean esPropietario) {
        if (!esPropietario) {
            throw new AccesoDenegadoException(MSG_SOLO_PROPIETARIO);
        }

        Negocio negocio = negocioRepository.findById(negocioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_NEGOCIO_NO_ENCONTRADO));

        if (!negocio.getPropietarioId().equals(usuarioIdAutenticado)) {
            throw new AccesoDenegadoException(MSG_SIN_PERMISO_NEGOCIO);
        }
        return negocio;
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(MSG_NOMBRE_OBLIGATORIO);
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        if (limpio.length() > NOMBRE_MAX) {
            throw new IllegalArgumentException(MSG_NOMBRE_LARGO);
        }
        return limpio;
    }

    private String validarTipo(String tipo) {
        String canonico = tipo == null ? null : TIPOS_PERMITIDOS.get(TextoNormalizador.normalizar(tipo));
        if (canonico == null) {
            throw new IllegalArgumentException(MSG_TIPO_INVALIDO);
        }
        return canonico;
    }

    /**
     * Acepta un entero JSON (3) o un texto con solo dígitos ("3").
     * Rechaza decimales (2.5), texto no numérico ("abc"), vacío o nulo, y valores fuera de 1..999.
     */
    private Integer validarCantidad(Object cantidad) {
        BigInteger valor = switch (cantidad) {
            case Integer i -> BigInteger.valueOf(i);
            case Long l -> BigInteger.valueOf(l);
            case Short s -> BigInteger.valueOf(s);
            case BigInteger b -> b;
            case String s when s.trim().matches("[+-]?\\d+") -> new BigInteger(s.trim());
            case null, default -> throw new IllegalArgumentException(MSG_CANTIDAD_INVALIDA);
        };
        if (valor.compareTo(CANTIDAD_MINIMA) < 0 || valor.compareTo(CANTIDAD_MAXIMA) > 0) {
            throw new IllegalArgumentException(MSG_CANTIDAD_INVALIDA);
        }
        return valor.intValue();
    }

    private String validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        String limpia = descripcion.trim();
        if (limpia.length() > DESCRIPCION_MAX) {
            throw new IllegalArgumentException(MSG_DESCRIPCION_LARGA);
        }
        return limpia;
    }

    private boolean tieneRolPropietarioEnContexto() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_PROPIETARIO")
                        || a.getAuthority().equalsIgnoreCase("PROPIETARIO"));
    }
}
