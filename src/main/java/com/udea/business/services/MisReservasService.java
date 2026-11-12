package com.udea.business.services;

import com.udea.business.controllers.DTOs.MisReservasResponse;
import com.udea.business.controllers.DTOs.ReservaResponse;
import com.udea.business.controllers.DTOs.ReservaResumenResponse;
import com.udea.business.entities.Reserva;
import com.udea.business.exceptions.AccesoDenegadoException;
import com.udea.business.exceptions.RecursoNoEncontradoException;
import com.udea.business.exceptions.SolicitudInvalidaException;
import com.udea.business.repositories.ReservaRepository;
import com.udea.business.security.IUsuarioActual;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MisReservasService implements IMisReservasService {

    public static final String MSG_SIN_RESERVAS = "Aún no tienes reservas.";
    public static final String MSG_SOLO_CLIENTES = "Solo los clientes pueden consultar sus reservas.";
    public static final String MSG_ID_OBLIGATORIO = "El identificador de la reserva es obligatorio.";
    public static final String MSG_RESERVA_NO_ENCONTRADA = "Reserva no encontrada.";

    private final ReservaRepository reservaRepository;
    private final IClasificadorReservas clasificador;
    private final IReservaConsultaMapper mapper;
    private final IUsuarioActual usuarioActual;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public MisReservasResponse consultarMisReservas() {
        Integer clienteId = clienteAutenticado();
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<Reserva> reservas = reservaRepository.findDetalladasPorCliente(clienteId);
        IClasificadorReservas.ReservasClasificadas grupos = clasificador.clasificar(reservas, ahora);

        return MisReservasResponse.builder()
                .mensaje(reservas.isEmpty() ? MSG_SIN_RESERVAS : null)
                .proximas(aResumenes(grupos.proximas(), ahora))
                .historial(aResumenes(grupos.historial(), ahora))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservaResponse consultarDetalle(Integer idReserva) {
        if (idReserva == null) {
            throw new SolicitudInvalidaException(MSG_ID_OBLIGATORIO);
        }
        Integer clienteId = clienteAutenticado();

        // Se filtra por cliente en la consulta: una reserva ajena se trata igual que una inexistente,
        // así no se revela que existe (CA-07).
        Reserva reserva = reservaRepository.findDetalladaPorIdYCliente(idReserva, clienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException(MSG_RESERVA_NO_ENCONTRADA));

        return mapper.aDetalle(reserva, clasificador.estadoVisible(reserva, LocalDateTime.now(clock)));
    }

    private Integer clienteAutenticado() {
        if (!usuarioActual.esCliente()) {
            throw new AccesoDenegadoException(MSG_SOLO_CLIENTES);
        }
        return usuarioActual.getId();
    }

    private List<ReservaResumenResponse> aResumenes(List<Reserva> reservas, LocalDateTime ahora) {
        return reservas.stream()
                .map(r -> mapper.aResumen(r, clasificador.estadoVisible(r, ahora)))
                .toList();
    }
}
