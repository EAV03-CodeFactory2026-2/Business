package com.udea.business.services;

import com.udea.business.controllers.DTOs.AsociacionRecursoResponse;
import com.udea.business.controllers.DTOs.AsociarRecursoRequest;
import com.udea.business.controllers.DTOs.RecursoRequeridoResponse;
import com.udea.business.entities.Negocio;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.udea.business.services.ServicioRecursoService.MSG_CANTIDAD_INVALIDA;
import static com.udea.business.services.ServicioRecursoService.MSG_RECURSO_ASOCIADO;
import static com.udea.business.services.ServicioRecursoService.MSG_RECURSO_ID_OBLIGATORIO;
import static com.udea.business.services.ServicioRecursoService.MSG_RECURSO_NO_ENCONTRADO;
import static com.udea.business.services.ServicioRecursoService.MSG_RECURSO_OTRO_NEGOCIO;
import static com.udea.business.services.ServicioRecursoService.MSG_RECURSO_YA_ASOCIADO;
import static com.udea.business.services.ServicioRecursoService.MSG_SERVICIO_NO_ENCONTRADO;
import static com.udea.business.services.ServicioRecursoService.MSG_SIN_PERMISO;
import static com.udea.business.services.ServicioRecursoService.MSG_SOLO_PROPIETARIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioRecursoServiceTest {

    private static final Integer ANA_ID = 7;
    private static final Integer OTRO_PROPIETARIO_ID = 8;
    private static final Integer SERVICIO_ID = 10;
    private static final Integer SPA_ARMONIA_ID = 5;
    private static final Integer SPA_NORTE_ID = 6;
    private static final Integer CAMILLA_ID = 30;
    private static final Integer SALA_ID = 31;
    private static final Integer SAUNA_ID = 32;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private RecursoNegocioRepository recursoNegocioRepository;

    @Mock
    private ServicioRecursoRepository servicioRecursoRepository;

    @Mock
    private IUsuarioActual usuarioActual;

    @InjectMocks
    private ServicioRecursoService service;

    // HU-0045/CA-01
    @Test
    void debeAsociarLaCamillaAlServicio_cuandoLaCantidadNoSuperaLasUnidades() {
        Servicio masaje = servicioDeAna("Masaje relajante");
        RecursoNegocio camilla = recurso(CAMILLA_ID, SPA_ARMONIA_ID, "Camilla", 2);
        when(servicioRecursoRepository.existsById(new ServicioRecursoId(SERVICIO_ID, CAMILLA_ID))).thenReturn(false);
        when(recursoNegocioRepository.findById(CAMILLA_ID)).thenReturn(Optional.of(camilla));
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID))
                .thenReturn(List.of(asociacion(masaje, camilla, 1)));

        AsociacionRecursoResponse respuesta = service.asociarRecurso(SERVICIO_ID, solicitud(CAMILLA_ID, 1));

        assertEquals(MSG_RECURSO_ASOCIADO, respuesta.getMensaje());
        assertEquals(SERVICIO_ID, respuesta.getIdServicio());
        RecursoRequeridoResponse requerido = respuesta.getRecursosRequeridos().get(0);
        assertEquals(CAMILLA_ID, requerido.getIdRecurso());
        assertEquals("Camilla", requerido.getNombreRecurso());
        assertEquals((short) 1, requerido.getCantidadRequerida());
        ArgumentCaptor<ServicioRecurso> captor = ArgumentCaptor.forClass(ServicioRecurso.class);
        verify(servicioRecursoRepository).save(captor.capture());
        ServicioRecurso guardada = captor.getValue();
        assertEquals(new ServicioRecursoId(SERVICIO_ID, CAMILLA_ID), guardada.getId());
        assertSame(masaje, guardada.getServicio());
        assertSame(camilla, guardada.getRecursoNegocio());
        assertEquals(SPA_ARMONIA_ID, guardada.getNegocioId());
        assertEquals((short) 1, guardada.getCantidadRequerida());
    }

    @Test
    void debeAsociarTodasLasUnidades_cuandoLaCantidadEsIgualALasDisponibles() {
        servicioDeAna("Masaje en pareja");
        when(servicioRecursoRepository.existsById(new ServicioRecursoId(SERVICIO_ID, CAMILLA_ID))).thenReturn(false);
        when(recursoNegocioRepository.findById(CAMILLA_ID))
                .thenReturn(Optional.of(recurso(CAMILLA_ID, SPA_ARMONIA_ID, "Camilla", 2)));

        AsociacionRecursoResponse respuesta = service.asociarRecurso(SERVICIO_ID, solicitud(CAMILLA_ID, 2));

        assertEquals(MSG_RECURSO_ASOCIADO, respuesta.getMensaje());
        verify(servicioRecursoRepository).save(any(ServicioRecurso.class));
    }

    // HU-0045/CA-02
    @Test
    void debeListarDosCamillasYUnaSala_cuandoSeConsultaMasajeEnPareja() {
        Servicio pareja = servicioDeAna("Masaje en pareja");
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID)).thenReturn(List.of(
                asociacion(pareja, recurso(CAMILLA_ID, SPA_ARMONIA_ID, "Camilla", 2), 2),
                asociacion(pareja, recurso(SALA_ID, SPA_ARMONIA_ID, "Sala privada", 1), 1)));

        List<RecursoRequeridoResponse> recursos = service.consultarRecursosRequeridos(SERVICIO_ID);

        assertEquals(List.of("2 Camilla", "1 Sala privada"),
                recursos.stream().map(r -> r.getCantidadRequerida() + " " + r.getNombreRecurso()).toList());
    }

    @Test
    void debeDevolverListaVacia_cuandoElServicioNoRequiereRecursos() {
        servicioDeAna("Masaje relajante");
        when(servicioRecursoRepository.findByServicioIdWithRecurso(SERVICIO_ID)).thenReturn(List.of());

        assertTrue(service.consultarRecursosRequeridos(SERVICIO_ID).isEmpty());
    }

    // HU-0045/CA-03
    @Test
    void debeRechazarLaAsociacion_cuandoSePidenMasCamillasDeLasDisponibles() {
        servicioDeAna("Masaje relajante");
        when(servicioRecursoRepository.existsById(new ServicioRecursoId(SERVICIO_ID, CAMILLA_ID))).thenReturn(false);
        when(recursoNegocioRepository.findById(CAMILLA_ID))
                .thenReturn(Optional.of(recurso(CAMILLA_ID, SPA_ARMONIA_ID, "Camilla", 2)));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.asociarRecurso(SERVICIO_ID, solicitud(CAMILLA_ID, 3)));

        assertEquals("El recurso Camilla solo tiene 2 unidades.", ex.getMessage());
        verify(servicioRecursoRepository, never()).save(any());
    }

    // HU-0045/CA-04
    @Test
    void debeRechazarLaAsociacion_cuandoElRecursoEsDeOtroNegocioDelMismoPropietario() {
        servicioDeAna("Masaje relajante");
        when(servicioRecursoRepository.existsById(new ServicioRecursoId(SERVICIO_ID, SAUNA_ID))).thenReturn(false);
        when(recursoNegocioRepository.findById(SAUNA_ID))
                .thenReturn(Optional.of(recurso(SAUNA_ID, SPA_NORTE_ID, "Sauna", 1)));

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.asociarRecurso(SERVICIO_ID, solicitud(SAUNA_ID, 1)));

        assertEquals(MSG_RECURSO_OTRO_NEGOCIO, ex.getMessage());
        verify(servicioRecursoRepository, never()).save(any());
    }

    // HU-0045/CA-05
    @Test
    void debeRechazarLaAsociacion_cuandoElRecursoYaEstaAsociado() {
        servicioDeAna("Masaje relajante");
        when(servicioRecursoRepository.existsById(new ServicioRecursoId(SERVICIO_ID, CAMILLA_ID))).thenReturn(true);

        RecursoDuplicadoException ex = assertThrows(RecursoDuplicadoException.class,
                () -> service.asociarRecurso(SERVICIO_ID, solicitud(CAMILLA_ID, 1)));

        assertEquals(MSG_RECURSO_YA_ASOCIADO, ex.getMessage());
        verify(servicioRecursoRepository, never()).save(any());
        verifyNoInteractions(recursoNegocioRepository);
    }

    // HU-0045/CA-06
    @Test
    void debeQuitarLaCamilla_cuandoElServicioLaRequiere() {
        servicioDeAna("Masaje relajante");
        ServicioRecursoId pk = new ServicioRecursoId(SERVICIO_ID, CAMILLA_ID);
        when(servicioRecursoRepository.existsById(pk)).thenReturn(true);

        service.quitarRecurso(SERVICIO_ID, CAMILLA_ID);

        verify(servicioRecursoRepository).deleteById(pk);
    }

    @Test
    void debeResponderRecursoNoEncontrado_cuandoSeQuitaUnRecursoNoAsociado() {
        servicioDeAna("Masaje relajante");
        when(servicioRecursoRepository.existsById(new ServicioRecursoId(SERVICIO_ID, CAMILLA_ID))).thenReturn(false);

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.quitarRecurso(SERVICIO_ID, CAMILLA_ID));

        assertEquals(MSG_RECURSO_NO_ENCONTRADO, ex.getMessage());
        verify(servicioRecursoRepository, never()).deleteById(any());
    }

    @Test
    void debeRechazarQuitar_cuandoNoSeIndicaElRecurso() {
        servicioDeAna("Masaje relajante");

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.quitarRecurso(SERVICIO_ID, null));

        assertEquals(MSG_RECURSO_ID_OBLIGATORIO, ex.getMessage());
    }

    // HU-0045/CA-07
    @Test
    void debeRechazarLaAsociacion_cuandoElServicioEsDeOtroPropietario() {
        servicioDe(OTRO_PROPIETARIO_ID);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> service.asociarRecurso(SERVICIO_ID, solicitud(CAMILLA_ID, 1)));

        assertEquals(MSG_SIN_PERMISO, ex.getMessage());
        verifyNoInteractions(servicioRecursoRepository, recursoNegocioRepository);
    }

    // HU-0045/CA-07
    @Test
    void debeRechazarConsultarYQuitar_cuandoElServicioEsDeOtroPropietario() {
        servicioDe(OTRO_PROPIETARIO_ID);

        AccesoDenegadoException consultar = assertThrows(AccesoDenegadoException.class,
                () -> service.consultarRecursosRequeridos(SERVICIO_ID));
        AccesoDenegadoException quitar = assertThrows(AccesoDenegadoException.class,
                () -> service.quitarRecurso(SERVICIO_ID, CAMILLA_ID));

        assertEquals(MSG_SIN_PERMISO, consultar.getMessage());
        assertEquals(MSG_SIN_PERMISO, quitar.getMessage());
        verifyNoInteractions(servicioRecursoRepository);
    }

    @Test
    void debeRechazarLaGestion_cuandoElUsuarioNoEsPropietario() {
        when(usuarioActual.esPropietario()).thenReturn(false);

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> service.asociarRecurso(SERVICIO_ID, solicitud(CAMILLA_ID, 1)));

        assertEquals(MSG_SOLO_PROPIETARIO, ex.getMessage());
        verifyNoInteractions(servicioRepository);
    }

    @Test
    void debeResponderServicioNoEncontrado_cuandoElServicioNoExiste() {
        when(usuarioActual.esPropietario()).thenReturn(true);
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.consultarRecursosRequeridos(SERVICIO_ID));

        assertEquals(MSG_SERVICIO_NO_ENCONTRADO, ex.getMessage());
    }

    @Test
    void debeRechazarLaGestion_cuandoNoSeIndicaElServicio() {
        when(usuarioActual.esPropietario()).thenReturn(true);

        SolicitudInvalidaException ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.consultarRecursosRequeridos(null));

        assertEquals("El identificador del servicio es obligatorio.", ex.getMessage());
        verifyNoInteractions(servicioRepository);
    }

    @Test
    void debeRechazarLaAsociacion_cuandoLaSolicitudNoTraeRecurso() {
        servicioDeAna("Masaje relajante");

        SolicitudInvalidaException sinCuerpo = assertThrows(SolicitudInvalidaException.class,
                () -> service.asociarRecurso(SERVICIO_ID, null));
        SolicitudInvalidaException sinRecurso = assertThrows(SolicitudInvalidaException.class,
                () -> service.asociarRecurso(SERVICIO_ID, solicitud(null, 1)));

        assertEquals(MSG_RECURSO_ID_OBLIGATORIO, sinCuerpo.getMessage());
        assertEquals(MSG_RECURSO_ID_OBLIGATORIO, sinRecurso.getMessage());
    }

    @Test
    void debeRechazarLaAsociacion_cuandoLaCantidadEsMenorQueUno() {
        servicioDeAna("Masaje relajante");
        AsociarRecursoRequest sinCantidad = AsociarRecursoRequest.builder().recursoId(CAMILLA_ID).build();

        SolicitudInvalidaException cero = assertThrows(SolicitudInvalidaException.class,
                () -> service.asociarRecurso(SERVICIO_ID, solicitud(CAMILLA_ID, 0)));
        SolicitudInvalidaException nula = assertThrows(SolicitudInvalidaException.class,
                () -> service.asociarRecurso(SERVICIO_ID, sinCantidad));

        assertEquals(MSG_CANTIDAD_INVALIDA, cero.getMessage());
        assertEquals(MSG_CANTIDAD_INVALIDA, nula.getMessage());
        verifyNoInteractions(servicioRecursoRepository);
    }

    @Test
    void debeResponderRecursoNoEncontrado_cuandoElRecursoNoExiste() {
        servicioDeAna("Masaje relajante");
        when(servicioRecursoRepository.existsById(new ServicioRecursoId(SERVICIO_ID, 99))).thenReturn(false);
        when(recursoNegocioRepository.findById(99)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> service.asociarRecurso(SERVICIO_ID, solicitud(99, 1)));

        assertEquals(MSG_RECURSO_NO_ENCONTRADO, ex.getMessage());
    }

    private Servicio servicioDeAna(String nombre) {
        Servicio servicio = servicioDe(ANA_ID);
        servicio.setNombre(nombre);
        return servicio;
    }

    private Servicio servicioDe(Integer propietarioId) {
        when(usuarioActual.esPropietario()).thenReturn(true);
        when(usuarioActual.getId()).thenReturn(ANA_ID);
        Servicio servicio = Servicio.builder()
                .idServicio(SERVICIO_ID)
                .negocio(Negocio.builder()
                        .idNegocio(SPA_ARMONIA_ID)
                        .propietarioId(propietarioId)
                        .nombreNegocio(ANA_ID.equals(propietarioId) ? "Spa Armonía" : "Barbería Sur")
                        .build())
                .build();
        when(servicioRepository.findById(SERVICIO_ID)).thenReturn(Optional.of(servicio));
        return servicio;
    }

    private AsociarRecursoRequest solicitud(Integer recursoId, int cantidad) {
        return AsociarRecursoRequest.builder().recursoId(recursoId).cantidadRequerida((short) cantidad).build();
    }

    private RecursoNegocio recurso(Integer id, Integer negocioId, String nombre, int unidades) {
        return RecursoNegocio.builder()
                .idRecurso(id)
                .negocioId(negocioId)
                .nombre(nombre)
                .cantidad(unidades)
                .estado("Disponible")
                .build();
    }

    private ServicioRecurso asociacion(Servicio servicio, RecursoNegocio recurso, int unidades) {
        return ServicioRecurso.builder()
                .id(new ServicioRecursoId(servicio.getIdServicio(), recurso.getIdRecurso()))
                .servicio(servicio)
                .recursoNegocio(recurso)
                .negocioId(recurso.getNegocioId())
                .cantidadRequerida((short) unidades)
                .build();
    }
}
