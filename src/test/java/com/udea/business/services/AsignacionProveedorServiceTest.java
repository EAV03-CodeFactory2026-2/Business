package com.udea.business.services;

import com.udea.business.controllers.DTOs.AsignacionProveedoresResponse;
import com.udea.business.controllers.DTOs.ProveedorAsignadoResponse;
import com.udea.business.entities.EstadoServicio;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.PerfilProveedor;
import com.udea.business.entities.RolPlataforma;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.ServicioProveedor;
import com.udea.business.entities.TipoDocumento;
import com.udea.business.entities.UsuarioPlataforma;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.PerfilProveedorRepository;
import com.udea.business.repositories.ServicioProveedorRepository;
import com.udea.business.repositories.ServicioRepository;
import com.udea.business.security.IUsuarioActual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.udea.business.services.AsignacionProveedorService.MSG_ASIGNADO;
import static com.udea.business.services.AsignacionProveedorService.MSG_ID_INVALIDO;
import static com.udea.business.services.AsignacionProveedorService.MSG_NO_SON_PROVEEDORES;
import static com.udea.business.services.AsignacionProveedorService.MSG_REPETIDOS;
import static com.udea.business.services.AsignacionProveedorService.MSG_SERVICIO_NO_ENCONTRADO;
import static com.udea.business.services.AsignacionProveedorService.MSG_SIN_PERMISO;
import static com.udea.business.services.AsignacionProveedorService.MSG_SIN_SELECCION;
import static com.udea.business.services.AsignacionProveedorService.MSG_SOLO_PROPIETARIO;
import static com.udea.business.services.AsignacionProveedorService.MSG_YA_VINCULADOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionProveedorServiceTest {

    private static final Integer SERVICIO_ID = 10;
    private static final Integer PROPIETARIO_ID = 7;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private PerfilProveedorRepository perfilProveedorRepository;

    @Mock
    private ServicioProveedorRepository servicioProveedorRepository;

    @Mock
    private IUsuarioActual usuarioActual;

    @InjectMocks
    private AsignacionProveedorService service;

    @Captor
    private ArgumentCaptor<List<ServicioProveedor>> vinculacionesCaptor;

    @Test
    void asignarVinculaLosProveedoresYDejaElServicioAsignado() {
        Servicio servicio = servicioDelPropietarioAutenticado();
        PerfilProveedor ana = perfil(12, "Ana", "Gomez", "1001");
        PerfilProveedor luis = perfil(15, "Luis", "Perez", "1002");
        when(perfilProveedorRepository.buscarActivosPorIds(Set.of(12, 15), RolPlataforma.PROVEEDOR))
                .thenReturn(List.of(ana, luis));
        when(servicioProveedorRepository.buscarProveedoresYaVinculados(SERVICIO_ID, List.of(12, 15)))
                .thenReturn(List.of());
        when(servicioProveedorRepository.countByServicioIdServicio(SERVICIO_ID)).thenReturn(2L);

        AsignacionProveedoresResponse respuesta = service.asignar(SERVICIO_ID, List.of(12, 15));

        assertEquals(String.format(MSG_ASIGNADO, 2), respuesta.getMensaje());
        assertEquals(SERVICIO_ID, respuesta.getIdServicio());
        assertEquals(EstadoServicio.ASIGNADO, respuesta.getEstadoServicio());
        assertEquals(2L, respuesta.getTotalProveedores());
        ProveedorAsignadoResponse primero = respuesta.getProveedoresAsignados().get(0);
        assertEquals(12, primero.getIdUsuario());
        assertEquals("Ana", primero.getNombre());
        assertEquals("Gomez", primero.getApellido());
        assertEquals("Masajes", primero.getEspecialidad());
        assertEquals("CC", primero.getTipoDocumento());
        assertEquals("1001", primero.getNumeroDocumento());
        verify(servicioProveedorRepository).saveAllAndFlush(vinculacionesCaptor.capture());
        List<ServicioProveedor> vinculaciones = vinculacionesCaptor.getValue();
        assertEquals(List.of(ana, luis), vinculaciones.stream().map(ServicioProveedor::getProveedor).toList());
        assertTrue(vinculaciones.stream().allMatch(v -> v.getServicio() == servicio));
        verify(servicioRepository).save(servicio);
        assertEquals(EstadoServicio.ASIGNADO, servicio.getEstado());
    }

    @Test
    void asignarRespetaElOrdenEnQueSePidieronLosProveedores() {
        servicioDelPropietarioAutenticado();
        PerfilProveedor ana = perfil(12, "Ana", "Gomez", "1001");
        PerfilProveedor luis = perfil(15, "Luis", "Perez", "1002");
        when(perfilProveedorRepository.buscarActivosPorIds(Set.of(12, 15), RolPlataforma.PROVEEDOR))
                .thenReturn(List.of(ana, luis));
        when(servicioProveedorRepository.buscarProveedoresYaVinculados(SERVICIO_ID, List.of(15, 12)))
                .thenReturn(List.of());
        when(servicioProveedorRepository.countByServicioIdServicio(SERVICIO_ID)).thenReturn(2L);

        AsignacionProveedoresResponse respuesta = service.asignar(SERVICIO_ID, List.of(15, 12));

        assertEquals(List.of(15, 12), idsAsignados(respuesta));
        verify(servicioProveedorRepository).saveAllAndFlush(vinculacionesCaptor.capture());
        assertEquals(List.of(luis, ana),
                vinculacionesCaptor.getValue().stream().map(ServicioProveedor::getProveedor).toList());
    }

    @Test
    void asignarSumaAlTotalLosProveedoresQueYaTeniaElServicio() {
        servicioDelPropietarioAutenticado();
        when(perfilProveedorRepository.buscarActivosPorIds(Set.of(12, 15), RolPlataforma.PROVEEDOR))
                .thenReturn(List.of(perfil(12, "Ana", "Gomez", "1001"), perfil(15, "Luis", "Perez", "1002")));
        when(servicioProveedorRepository.buscarProveedoresYaVinculados(SERVICIO_ID, List.of(12, 15)))
                .thenReturn(List.of());
        when(servicioProveedorRepository.countByServicioIdServicio(SERVICIO_ID)).thenReturn(3L);

        AsignacionProveedoresResponse respuesta = service.asignar(SERVICIO_ID, List.of(12, 15));

        assertEquals(String.format(MSG_ASIGNADO, 2), respuesta.getMensaje());
        assertEquals(3L, respuesta.getTotalProveedores());
        assertEquals(EstadoServicio.ASIGNADO, respuesta.getEstadoServicio());
        assertEquals(List.of(12, 15), idsAsignados(respuesta));
    }

    @Test
    void asignarPermiteVincularAlPropietarioCuandoTambienEsProveedorActivo() {
        Servicio servicio = servicioDelPropietarioAutenticado();
        when(perfilProveedorRepository.buscarActivosPorIds(Set.of(PROPIETARIO_ID, 12), RolPlataforma.PROVEEDOR))
                .thenReturn(List.of(perfil(PROPIETARIO_ID, "Carlos", "Ruiz", "1000"), perfil(12, "Ana", "Gomez", "1001")));
        when(servicioProveedorRepository.buscarProveedoresYaVinculados(SERVICIO_ID, List.of(PROPIETARIO_ID, 12)))
                .thenReturn(List.of());
        when(servicioProveedorRepository.countByServicioIdServicio(SERVICIO_ID)).thenReturn(2L);

        AsignacionProveedoresResponse respuesta = service.asignar(SERVICIO_ID, List.of(PROPIETARIO_ID, 12));

        assertEquals(String.format(MSG_ASIGNADO, 2), respuesta.getMensaje());
        assertEquals(List.of(PROPIETARIO_ID, 12), idsAsignados(respuesta));
        verify(servicioRepository).save(servicio);
    }

    @Test
    void asignarRetorna403CuandoElUsuarioNoEsPropietario() {
        when(usuarioActual.esPropietario()).thenReturn(false);
        List<Integer> ids = List.of(12);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(MSG_SOLO_PROPIETARIO, ex.getMessage());
        verify(servicioRepository, never()).findById(any());
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna403AntesDeValidarLaSeleccionCuandoNoEsPropietario() {
        when(usuarioActual.esPropietario()).thenReturn(false);
        List<Integer> ids = List.of();

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(MSG_SOLO_PROPIETARIO, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository);
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna404CuandoElServicioNoExiste() {
        when(usuarioActual.esPropietario()).thenReturn(true);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.empty());
        List<Integer> ids = List.of(12);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(MSG_SERVICIO_NO_ENCONTRADO, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository);
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna403CuandoElServicioEsDeOtroPropietario() {
        when(usuarioActual.esPropietario()).thenReturn(true);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio(99)));
        when(usuarioActual.getId()).thenReturn(PROPIETARIO_ID);
        List<Integer> ids = List.of(12);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(MSG_SIN_PERMISO, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository);
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna400CuandoLaSeleccionEsNula() {
        servicioDelPropietarioAutenticado();

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.asignar(SERVICIO_ID, null));

        assertEquals(MSG_SIN_SELECCION, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository);
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna400CuandoLaSeleccionEstaVacia() {
        servicioDelPropietarioAutenticado();
        List<Integer> ids = List.of();

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(MSG_SIN_SELECCION, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository);
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna400CuandoLaSeleccionContieneUnIdNulo() {
        servicioDelPropietarioAutenticado();
        List<Integer> ids = Arrays.asList(12, null);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(MSG_ID_INVALIDO, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository);
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna400CuandoHayProveedoresRepetidos() {
        servicioDelPropietarioAutenticado();
        List<Integer> ids = List.of(12, 12);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(MSG_REPETIDOS, ex.getMessage());
        verifyNoInteractions(perfilProveedorRepository);
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna400CuandoUnIdNoEsProveedorActivo() {
        servicioDelPropietarioAutenticado();
        when(perfilProveedorRepository.buscarActivosPorIds(Set.of(12, 15), RolPlataforma.PROVEEDOR))
                .thenReturn(List.of(perfil(12, "Ana", "Gomez", "1001")));
        List<Integer> ids = List.of(12, 15);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(String.format(MSG_NO_SON_PROVEEDORES, "15"), ex.getMessage());
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna400ListandoTodosLosIdsQueNoSonProveedores() {
        servicioDelPropietarioAutenticado();
        when(perfilProveedorRepository.buscarActivosPorIds(Set.of(12, 15, 20), RolPlataforma.PROVEEDOR))
                .thenReturn(List.of(perfil(12, "Ana", "Gomez", "1001")));
        List<Integer> ids = List.of(12, 15, 20);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(String.format(MSG_NO_SON_PROVEEDORES, "15, 20"), ex.getMessage());
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna409CuandoProveedorYaEstaVinculado() {
        servicioDelPropietarioAutenticado();
        when(perfilProveedorRepository.buscarActivosPorIds(Set.of(12, 15), RolPlataforma.PROVEEDOR))
                .thenReturn(List.of(perfil(12, "Ana", "Gomez", "1001"), perfil(15, "Luis", "Perez", "1002")));
        when(servicioProveedorRepository.buscarProveedoresYaVinculados(SERVICIO_ID, List.of(12, 15)))
                .thenReturn(List.of(15));
        List<Integer> ids = List.of(12, 15);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(String.format(MSG_YA_VINCULADOS, "Luis Perez (CC 1002)"), ex.getMessage());
        verificarQueNoSePersistioNada();
    }

    @Test
    void asignarRetorna409CuandoOtraPeticionVinculaElMismoProveedorEnParalelo() {
        servicioDelPropietarioAutenticado();
        when(perfilProveedorRepository.buscarActivosPorIds(Set.of(12), RolPlataforma.PROVEEDOR))
                .thenReturn(List.of(perfil(12, "Ana", "Gomez", "1001")));
        when(servicioProveedorRepository.buscarProveedoresYaVinculados(SERVICIO_ID, List.of(12)))
                .thenReturn(List.of());
        when(servicioProveedorRepository.saveAllAndFlush(anyList()))
                .thenThrow(new DataIntegrityViolationException("uk_servicio_proveedor"));
        List<Integer> ids = List.of(12);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> service.asignar(SERVICIO_ID, ids));

        assertEquals(String.format(MSG_YA_VINCULADOS, "uno de los proveedores seleccionados"), ex.getMessage());
        verify(servicioProveedorRepository, never()).countByServicioIdServicio(any());
        verify(servicioRepository, never()).save(any());
    }

    private Servicio servicioDelPropietarioAutenticado() {
        Servicio servicio = servicio(PROPIETARIO_ID);
        when(usuarioActual.esPropietario()).thenReturn(true);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));
        when(usuarioActual.getId()).thenReturn(PROPIETARIO_ID);
        return servicio;
    }

    private Servicio servicio(Integer propietarioId) {
        Negocio negocio = Negocio.builder()
                .idNegocio(1)
                .propietarioId(propietarioId)
                .nombreNegocio("Spa Central")
                .build();
        return Servicio.builder()
                .idServicio(SERVICIO_ID)
                .negocio(negocio)
                .nombre("Masaje relajante")
                .estado(EstadoServicio.NO_ASIGNADO)
                .build();
    }

    private PerfilProveedor perfil(Integer usuarioId, String nombre, String apellido, String documento) {
        TipoDocumento cedula = TipoDocumento.builder()
                .idTipoDocumento(1)
                .codigo("CC")
                .nombre("Cedula de ciudadania")
                .build();
        return PerfilProveedor.builder()
                .usuarioId(usuarioId)
                .usuario(usuario(usuarioId, nombre, apellido))
                .tipoDocumento(cedula)
                .numeroDocumento(documento)
                .especialidad("Masajes")
                .build();
    }

    private UsuarioPlataforma usuario(Integer idUsuario, String nombre, String apellido) {
        return UsuarioPlataforma.builder()
                .idUsuario(idUsuario)
                .nombre(nombre)
                .apellido(apellido)
                .estado("Activo")
                .build();
    }

    private List<Integer> idsAsignados(AsignacionProveedoresResponse respuesta) {
        return respuesta.getProveedoresAsignados().stream()
                .map(ProveedorAsignadoResponse::getIdUsuario)
                .toList();
    }

    private void verificarQueNoSePersistioNada() {
        verify(servicioProveedorRepository, never()).saveAllAndFlush(anyList());
        verify(servicioRepository, never()).save(any());
    }
}
