package com.udea.business.services;

import com.udea.business.controllers.DTOs.AgendaProveedorResponse;
import com.udea.business.controllers.DTOs.ReservaAgendaResponse;
import com.udea.business.entities.EstadoReserva;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.security.IUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendaProveedorService implements IAgendaProveedorService {

    public static final String MSG_SIN_RESERVAS = "No tienes reservas para este día.";
    public static final String MSG_SOLO_PROVEEDORES = "Solo los proveedores pueden consultar su agenda.";

    private final ReservaRepository reservaRepository;
    private final IReservaConsultaMapper mapper;
    private final IUsuarioActual usuarioActual;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public AgendaProveedorResponse consultarMiAgenda(LocalDate fecha) {
        if (!usuarioActual.esProveedor()) {
            throw new AccesoDenegadoException(MSG_SOLO_PROVEEDORES);
        }
        // El proveedor siempre es el del token: no se acepta un id externo (solo ve su propia agenda).
        Integer proveedorId = usuarioActual.getId();
        LocalDate dia = fecha != null ? fecha : LocalDate.now(clock);

        List<ReservaAgendaResponse> reservas = reservaRepository.findAgendaProveedor(
                        proveedorId, dia.atStartOfDay(), dia.plusDays(1).atStartOfDay(), EstadoReserva.CONFIRMADA)
                .stream()
                .map(mapper::aAgenda)
                .toList();

        return AgendaProveedorResponse.builder()
                .fecha(dia.format(DateTimeFormatter.ISO_LOCAL_DATE))
                .totalReservas(reservas.size())
                .mensaje(reservas.isEmpty() ? MSG_SIN_RESERVAS : null)
                .reservas(reservas)
                .build();
    }
}
