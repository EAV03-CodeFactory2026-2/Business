package com.udea.business.services.acceso;

import com.udea.business.entities.Negocio;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.security.IUsuarioActual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccesoReservaPolicyTest {

    private static final Integer LAURA_ID = 20;
    private static final Integer MARIO_ID = 21;
    private static final Integer CARLOS_ID = 12;
    private static final Integer DIEGO_ID = 15;
    private static final Integer ANA_ID = 7;
    private static final Integer OTRO_PROPIETARIO_ID = 8;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private IUsuarioActual usuario;

    // HU-0019/CA-07
    @Test
    void debePermitirVer_cuandoLaClienteEsLaDuenaDeLaReserva() {
        con(LAURA_ID, true, false, false);

        assertTrue(politica().puedeVer(reserva()));
    }

    // HU-0019/CA-07
    @Test
    void debeOcultarLaReserva_cuandoLaPideOtroCliente() {
        con(MARIO_ID, true, false, false);

        assertFalse(politica().puedeVer(reserva()));
    }

    @Test
    void debePermitirVer_cuandoElProveedorAtiendeLaReserva() {
        con(CARLOS_ID, false, true, false);

        assertTrue(politica().puedeVer(reserva()));
    }

    @Test
    void debeOcultarLaReserva_cuandoLaPideOtroProveedor() {
        con(DIEGO_ID, false, true, false);

        assertFalse(politica().puedeVer(reserva()));
    }

    @Test
    void debePermitirVer_cuandoElPropietarioEsDuenoDelNegocioDelServicio() {
        con(ANA_ID, false, false, true);

        assertTrue(politica().puedeVer(reserva()));
    }

    @Test
    void debeOcultarLaReserva_cuandoElPropietarioEsDeOtroNegocio() {
        con(OTRO_PROPIETARIO_ID, false, false, true);

        assertFalse(politica().puedeVer(reserva()));
    }

    @Test
    void debeOcultarLaReserva_cuandoNoSeConoceElServicioOElNegocio() {
        con(ANA_ID, false, false, true);
        Reserva sinServicio = reserva();
        sinServicio.setServicio(null);
        Reserva sinNegocio = reserva();
        sinNegocio.setServicio(Servicio.builder().idServicio(1).build());

        assertFalse(politica().puedeVer(sinServicio));
        assertFalse(politica().puedeVer(sinNegocio));
    }

    @Test
    void debePermitirVer_cuandoAlgunoDeLosRolesDelUsuarioLoAutoriza() {
        con(CARLOS_ID, true, true, false);

        assertTrue(politica().puedeVer(reserva()));
    }

    @Test
    void debeOcultarLaReserva_cuandoElUsuarioNoTieneRolesDeReserva() {
        con(LAURA_ID, false, false, false);

        assertFalse(politica().puedeVer(reserva()));
    }

    private AccesoReservaPolicy politica() {
        return new AccesoReservaPolicy(
                List.of(new ReglaAccesoCliente(), new ReglaAccesoProveedor(), new ReglaAccesoPropietario()),
                usuario);
    }

    private void con(Integer id, boolean cliente, boolean proveedor, boolean propietario) {
        when(usuario.getId()).thenReturn(id);
        when(usuario.esCliente()).thenReturn(cliente);
        when(usuario.esProveedor()).thenReturn(proveedor);
        when(usuario.esPropietario()).thenReturn(propietario);
    }

    private Reserva reserva() {
        Negocio negocio = Negocio.builder().idNegocio(5).propietarioId(ANA_ID).nombreNegocio("Barbería Centro").build();
        return Reserva.builder()
                .idReserva(100)
                .clienteId(LAURA_ID)
                .proveedorId(CARLOS_ID)
                .servicioId(1)
                .servicio(Servicio.builder().idServicio(1).negocio(negocio).build())
                .build();
    }
}
