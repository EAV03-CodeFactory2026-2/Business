package com.udea.business.services;

import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.EstadoVisibleReserva;
import com.udea.business.entities.Reserva;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ClasificadorReservas implements IClasificadorReservas {

    private static final Comparator<Reserva> POR_INICIO = Comparator.comparing(Reserva::getInicio);

    @Override
    public EstadoVisibleReserva estadoVisible(Reserva reserva, LocalDateTime ahora) {
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            return EstadoVisibleReserva.CANCELADA;
        }
        // Una reserva confirmada deja de ser "próxima" en cuanto pasa su hora de inicio.
        return reserva.getInicio().isAfter(ahora)
                ? EstadoVisibleReserva.CONFIRMADA
                : EstadoVisibleReserva.REALIZADA;
    }

    @Override
    public ReservasClasificadas clasificar(List<Reserva> reservas, LocalDateTime ahora) {
        Map<Boolean, List<Reserva>> grupos = reservas.stream()
                .collect(Collectors.partitioningBy(r -> estadoVisible(r, ahora).esProxima()));

        List<Reserva> proximas = grupos.get(true).stream().sorted(POR_INICIO).toList();
        List<Reserva> historial = grupos.get(false).stream().sorted(POR_INICIO.reversed()).toList();
        return new ReservasClasificadas(proximas, historial);
    }
}
