package com.udea.business.services;

import com.udea.business.controllers.DTOs.RecursoCreateRequest;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.Recurso;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoDuplicadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.repositories.NegocioRepository;
import com.udea.business.repositories.RecursoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecursoServiceTest {

    @Mock
    private RecursoRepository recursoRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @InjectMocks
    private RecursoService recursoService;

    private Negocio spaArmonia;

    @BeforeEach
    void setUp() {
        spaArmonia = Negocio.builder()
                .idNegocio(5)
                .propietarioId(10)
                .nombreNegocio("Spa Armonía")
                .estado("Activo")
                .build();
    }

    private RecursoCreateRequest request(String nombre, String tipo, Object cantidad, String descripcion) {
        return RecursoCreateRequest.builder()
                .nombre(nombre)
                .tipo(tipo)
                .cantidad(cantidad)
                .descripcion(descripcion)
                .build();
    }

    private void negocioExiste() {
        when(negocioRepository.findById(5)).thenReturn(Optional.of(spaArmonia));
    }

    @Test
    void registrarRecursoExitosoQuedaActivoYAsociadoAlNegocio() {
        negocioExiste();
        when(recursoRepository.existsByNegocioIdNegocioAndNombreNormalizado(5, "camilla de masajes")).thenReturn(false);
        when(recursoRepository.saveAndFlush(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        Recurso resultado = recursoService.registrarRecurso(5,
                request("Camilla de masajes", "Equipo", 3, "Camilla plegable"), 10, true);

        assertEquals("Camilla de masajes", resultado.getNombre());
        assertEquals("camilla de masajes", resultado.getNombreNormalizado());
        assertEquals("Equipo", resultado.getTipo());
        assertEquals(3, resultado.getCantidad());
        assertEquals("Camilla plegable", resultado.getDescripcion());
        assertEquals("Activo", resultado.getEstado());
        assertSame(spaArmonia, resultado.getNegocio());
    }

    @Test
    void registrarRecursoSinDescripcionEsValido() {
        negocioExiste();
        when(recursoRepository.saveAndFlush(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        Recurso resultado = recursoService.registrarRecurso(5, request("Sala 2", "Espacio", 1, null), 10, true);

        assertNull(resultado.getDescripcion());
        assertEquals("Espacio", resultado.getTipo());
    }

    @Test
    void registrarRecursoDescripcionEnBlancoSeGuardaComoNula() {
        negocioExiste();
        when(recursoRepository.saveAndFlush(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        Recurso resultado = recursoService.registrarRecurso(5, request("Sala 2", "Espacio", 1, "   "), 10, true);

        assertNull(resultado.getDescripcion());
    }

    @Test
    void registrarRecursoAceptaCantidadComoTextoNumerico() {
        negocioExiste();
        when(recursoRepository.saveAndFlush(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        Recurso resultado = recursoService.registrarRecurso(5, request("Sala 3", "espacio", "12", null), 10, true);

        assertEquals(12, resultado.getCantidad());
        assertEquals("Espacio", resultado.getTipo());
    }

    @ParameterizedTest
    @ValueSource(strings = {"camilla de masajes", "CAMILLA DE MASAJES", "Camilla De Masajes", "Cámilla de masajes", "  camilla   de masajes "})
    void registrarRecursoRechazaNombreDuplicadoIgnorandoMayusculasYTildes(String nombre) {
        negocioExiste();
        when(recursoRepository.existsByNegocioIdNegocioAndNombreNormalizado(5, "camilla de masajes")).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> recursoService.registrarRecurso(5, request(nombre, "Equipo", 1, null), 10, true));

        assertEquals("Ya existe un recurso con este nombre en el negocio.", ex.getMessage());
        verify(recursoRepository, never()).saveAndFlush(any(Recurso.class));
    }

    @Test
    void registrarRecursoTraduceViolacionDeUnicidadEnBdADuplicado() {
        negocioExiste();
        when(recursoRepository.saveAndFlush(any(Recurso.class)))
                .thenThrow(new DataIntegrityViolationException("uk_recurso_negocio_nombre_normalizado"));

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> recursoService.registrarRecurso(5, request("Sala 1", "Espacio", 1, null), 10, true));

        assertEquals("Ya existe un recurso con este nombre en el negocio.", ex.getMessage());
    }

    static Stream<Arguments> cantidadesInvalidas() {
        return Stream.of(
                Arguments.of(0),
                Arguments.of(2.5),
                Arguments.of(-1),
                Arguments.of(1000),
                Arguments.of("abc"),
                Arguments.of((Object) null),
                Arguments.of(""),
                Arguments.of("2.5"),
                Arguments.of(99_999_999_999L),
                Arguments.of(true)
        );
    }

    @ParameterizedTest
    @MethodSource("cantidadesInvalidas")
    void registrarRecursoRechazaCantidadQueNoEsEnteroEntre1Y999(Object cantidad) {
        negocioExiste();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recursoService.registrarRecurso(5, request("Camilla", "Equipo", cantidad, null), 10, true));

        assertEquals("La cantidad debe ser un número entero entre 1 y 999.", ex.getMessage());
        verify(recursoRepository, never()).saveAndFlush(any(Recurso.class));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 999})
    void registrarRecursoAceptaLimitesDeCantidad(int cantidad) {
        negocioExiste();
        when(recursoRepository.saveAndFlush(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        Recurso resultado = recursoService.registrarRecurso(5, request("Silla", "Equipo", cantidad, null), 10, true);

        assertEquals(cantidad, resultado.getCantidad());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void registrarRecursoRechazaNombreVacio(String nombre) {
        negocioExiste();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recursoService.registrarRecurso(5, request(nombre, "Equipo", 1, null), 10, true));

        assertEquals("El nombre del recurso es obligatorio.", ex.getMessage());
    }

    @Test
    void registrarRecursoRechazaNombreDeMasDe100Caracteres() {
        negocioExiste();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recursoService.registrarRecurso(5, request("a".repeat(101), "Equipo", 1, null), 10, true));

        assertEquals("El nombre del recurso no puede exceder los 100 caracteres.", ex.getMessage());
    }

    @Test
    void registrarRecursoRechazaDescripcionDeMasDe500Caracteres() {
        negocioExiste();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recursoService.registrarRecurso(5, request("Sala", "Espacio", 1, "d".repeat(501)), 10, true));

        assertEquals("La descripción no puede exceder los 500 caracteres.", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mueble", "", "Servicio"})
    void registrarRecursoRechazaTipoInvalido(String tipo) {
        negocioExiste();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> recursoService.registrarRecurso(5, request("Sala", tipo, 1, null), 10, true));

        assertEquals("El tipo del recurso debe ser Espacio o Equipo.", ex.getMessage());
    }

    @Test
    void registrarRecursoRechazaTipoNulo() {
        negocioExiste();

        assertThrows(IllegalArgumentException.class,
                () -> recursoService.registrarRecurso(5, request("Sala", null, 1, null), 10, true));
    }

    @Test
    void registrarRecursoRechazaNegocioDeOtroPropietario() {
        negocioExiste();

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> recursoService.registrarRecurso(5, request("Sala", "Espacio", 1, null), 99, true));

        assertEquals("No tienes permiso para gestionar los recursos de este negocio.", ex.getMessage());
        verify(recursoRepository, never()).saveAndFlush(any(Recurso.class));
    }

    @Test
    void registrarRecursoRechazaUsuarioQueNoOperaComoPropietario() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> recursoService.registrarRecurso(5, request("Sala", "Espacio", 1, null), 10, false));

        assertEquals("Solo un Propietario puede gestionar los recursos del negocio.", ex.getMessage());
        verifyNoInteractions(negocioRepository, recursoRepository);
    }

    @Test
    void registrarRecursoValidaPermisosAntesQueDatos() {
        negocioExiste();

        // Datos inválidos en un negocio ajeno: se responde por permiso, no por validación.
        assertThrows(AccesoDenegadoException.class,
                () -> recursoService.registrarRecurso(5, request("", "X", "abc", null), 99, true));
    }

    @Test
    void registrarRecursoFallaSiNegocioNoExiste() {
        when(negocioRepository.findById(77)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> recursoService.registrarRecurso(77, request("Sala", "Espacio", 1, null), 10, true));

        assertEquals("Negocio no encontrado.", ex.getMessage());
    }

    @Test
    void registrarRecursoGuardaNombreLimpio() {
        negocioExiste();
        when(recursoRepository.saveAndFlush(any(Recurso.class))).thenAnswer(inv -> inv.getArgument(0));

        recursoService.registrarRecurso(5, request("  Sala   Única ", "Espacio", 1, null), 10, true);

        ArgumentCaptor<Recurso> captor = ArgumentCaptor.forClass(Recurso.class);
        verify(recursoRepository).saveAndFlush(captor.capture());
        assertEquals("Sala Única", captor.getValue().getNombre());
        assertEquals("sala unica", captor.getValue().getNombreNormalizado());
    }

    @Test
    void listarRecursosRetornaListaVaciaSiNoHay() {
        negocioExiste();
        when(recursoRepository.findByNegocioIdNegocioOrderByNombreAsc(5)).thenReturn(List.of());

        List<Recurso> resultado = recursoService.listarRecursos(5, 10, true);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarRecursosConsultaSoloElNegocioIndicado() {
        negocioExiste();
        Recurso camilla = Recurso.builder().idRecurso(1).negocio(spaArmonia).nombre("Camilla de masajes").cantidad(3).build();
        when(recursoRepository.findByNegocioIdNegocioOrderByNombreAsc(5)).thenReturn(List.of(camilla));

        List<Recurso> resultado = recursoService.listarRecursos(5, 10, true);

        assertEquals(List.of(camilla), resultado);
        verify(recursoRepository).findByNegocioIdNegocioOrderByNombreAsc(5);
        verify(recursoRepository, never()).findByNegocioIdNegocioOrderByNombreAsc(intThat(id -> id != 5));
    }

    @Test
    void listarRecursosRechazaNegocioDeOtroPropietario() {
        negocioExiste();

        assertThrows(AccesoDenegadoException.class, () -> recursoService.listarRecursos(5, 99, true));
        verify(recursoRepository, never()).findByNegocioIdNegocioOrderByNombreAsc(anyInt());
    }

    @Test
    void listarRecursosRechazaUsuarioQueNoOperaComoPropietario() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> recursoService.listarRecursos(5, 10, false));

        assertEquals("Solo un Propietario puede gestionar los recursos del negocio.", ex.getMessage());
        verify(recursoRepository, never()).existsByNegocioIdNegocioAndNombreNormalizado(anyInt(), anyString());
    }
}
