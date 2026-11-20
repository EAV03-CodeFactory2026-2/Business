package com.udea.business.services;

import com.udea.business.controllers.DTOs.ReservaAgendaResponse;
import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.controllers.DTOs.ReservaResumenResponse;
import com.udea.business.entities.CancelacionReserva;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.EstadoVisibleReserva;
import com.udea.business.entities.Negocio;
import com.udea.business.entities.PerfilProveedor;
import com.udea.business.entities.Reserva;
import com.udea.business.entities.Servicio;
import com.udea.business.entities.UsuarioPlataforma;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReservaConsultaMapperTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 9, 9, 0);
    private static final OffsetDateTime CREADA = OffsetDateTime.of(2026, 11, 5, 10, 0, 0, 0, ZoneOffset.ofHours(-5));

    private final ReservaConsultaMapper mapper = new ReservaConsultaMapper();

    @Test
    void debeMostrarFechaHoraServicioNegocioYEstado_cuandoSeArmaElResumen() {
        ReservaResumenResponse resumen = mapper.aResumen(reservaCompleta(), EstadoVisibleReserva.CONFIRMADA);

        assertEquals(100, resumen.getIdReserva());
        assertEquals("RSV-00000001", resumen.getCodigoReserva());
        assertEquals("2026-11-09", resumen.getFecha());
        assertEquals("09:00", resumen.getHoraInicio());
        assertEquals("09:30", resumen.getHoraFin());
        assertEquals("Corte clásico", resumen.getNombreServicio());
        assertEquals("Barbería Centro", resumen.getNombreNegocio());
        assertEquals("Confirmada", resumen.getEstado());
    }

    @Test
    void debeDejarElNegocioVacio_cuandoLaReservaNoTraeElServicio() {
        Reserva reserva = reservaCompleta();
        reserva.setServicio(null);

        ReservaResumenResponse resumen = mapper.aResumen(reserva, EstadoVisibleReserva.REALIZADA);

        assertNull(resumen.getNombreNegocio());
        assertEquals("Realizada", resumen.getEstado());
    }

    // HU-0019/CA-04
    @Test
    void debeMostrarElDetalleCompleto_cuandoLaReservaEstaConfirmada() {
        ReservaResponse detalle = mapper.aDetalle(reservaCompleta(), EstadoVisibleReserva.CONFIRMADA);

        assertEquals("RSV-00000001", detalle.getCodigoReserva());
        assertEquals("Confirmada", detalle.getEstado());
        assertEquals(5, detalle.getNegocioId());
        assertEquals("Barbería Centro", detalle.getNombreNegocio());
        assertEquals(1, detalle.getServicioId());
        assertEquals("Corte clásico", detalle.getNombreServicio());
        assertEquals(12, detalle.getProveedorId());
        assertEquals("Carlos Ruiz", detalle.getNombreProveedor());
        assertEquals("2026-11-09", detalle.getFecha());
        assertEquals("09:00", detalle.getHoraInicio());
        assertEquals("09:30", detalle.getHoraFin());
        assertEquals(30, detalle.getDuracionMinutos());
        assertEquals(new BigDecimal("25000.00"), detalle.getPrecio());
        assertEquals("COP", detalle.getMoneda());
        assertEquals("Prefiero corte con tijera", detalle.getNotas());
        assertEquals(CREADA, detalle.getFechaCreacion());
        assertNull(detalle.getFechaCancelacion());
        assertNull(detalle.getMotivoCancelacion());
    }

    // HU-0019/CA-05
    @Test
    void debeMostrarFechaYMotivoDeCancelacion_cuandoLaReservaFueCancelada() {
        Reserva reserva = reservaCompleta();
        reserva.setEstado(EstadoReserva.CANCELADA);
        OffsetDateTime cancelada = OffsetDateTime.of(2026, 11, 8, 18, 0, 0, 0, ZoneOffset.ofHours(-5));
        reserva.setCancelacion(CancelacionReserva.builder()
                .reservaId(100)
                .canceladaPor(20)
                .fechaCancelacion(cancelada)
                .motivo("Viaje de trabajo")
                .build());

        ReservaResponse detalle = mapper.aDetalle(reserva, EstadoVisibleReserva.CANCELADA);

        assertEquals("Cancelada", detalle.getEstado());
        assertEquals(cancelada, detalle.getFechaCancelacion());
        assertEquals("Viaje de trabajo", detalle.getMotivoCancelacion());
    }

    @Test
    void debeDejarNegocioYProveedorVacios_cuandoLaReservaNoTraeSusRelaciones() {
        Reserva reserva = reservaCompleta();
        reserva.setServicio(Servicio.builder().idServicio(1).build());
        reserva.setPerfilProveedor(PerfilProveedor.builder().usuarioId(12).build());

        ReservaResponse detalle = mapper.aDetalle(reserva, EstadoVisibleReserva.CONFIRMADA);

        assertNull(detalle.getNegocioId());
        assertNull(detalle.getNombreNegocio());
        assertNull(detalle.getNombreProveedor());
    }

    @Test
    void debeDejarElProveedorVacio_cuandoLaReservaNoTraeElPerfil() {
        Reserva reserva = reservaCompleta();
        reserva.setPerfilProveedor(null);

        ReservaResponse detalle = mapper.aDetalle(reserva, EstadoVisibleReserva.CONFIRMADA);

        assertNull(detalle.getNombreProveedor());
    }

    // HU-0022/CA-02
    @Test
    void debeIncluirContactoYNotasDelCliente_cuandoSeArmaLaAgenda() {
        ReservaAgendaResponse agenda = mapper.aAgenda(reservaCompleta());

        assertEquals(100, agenda.getIdReserva());
        assertEquals("RSV-00000001", agenda.getCodigoReserva());
        assertEquals("09:00", agenda.getHoraInicio());
        assertEquals("09:30", agenda.getHoraFin());
        assertEquals("Corte clásico", agenda.getNombreServicio());
        assertEquals("Barbería Centro", agenda.getNombreNegocio());
        assertEquals("Laura Pérez", agenda.getNombreCliente());
        assertEquals("+573001234567", agenda.getTelefonoCliente());
        assertEquals("Prefiero corte con tijera", agenda.getNotas());
    }

    @Test
    void debeDejarElContactoVacio_cuandoLaReservaNoTraeAlCliente() {
        Reserva reserva = reservaCompleta();
        reserva.setCliente(null);

        ReservaAgendaResponse agenda = mapper.aAgenda(reserva);

        assertNull(agenda.getNombreCliente());
        assertNull(agenda.getTelefonoCliente());
    }

    private Reserva reservaCompleta() {
        Negocio negocio = Negocio.builder().idNegocio(5).propietarioId(7).nombreNegocio("Barbería Centro").build();
        UsuarioPlataforma carlos = UsuarioPlataforma.builder().idUsuario(12).nombre("Carlos").apellido("Ruiz").build();
        UsuarioPlataforma laura = UsuarioPlataforma.builder()
                .idUsuario(20).nombre("Laura").apellido("Pérez").telefono("+573001234567").build();
        return Reserva.builder()
                .idReserva(100)
                .codigoReserva("RSV-00000001")
                .clienteId(20)
                .proveedorId(12)
                .servicioId(1)
                .inicio(INICIO)
                .fin(INICIO.plusMinutes(30))
                .notas("Prefiero corte con tijera")
                .nombreServicio("Corte clásico")
                .duracionMinutos(30)
                .precioServicio(new BigDecimal("25000.00"))
                .monedaCodigoIso("COP")
                .estado(EstadoReserva.CONFIRMADA)
                .fechaCreacion(CREADA)
                .servicio(Servicio.builder().idServicio(1).nombre("Corte clásico").negocio(negocio).build())
                .perfilProveedor(PerfilProveedor.builder().usuarioId(12).usuario(carlos).build())
                .cliente(laura)
                .build();
    }
}
