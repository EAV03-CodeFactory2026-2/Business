package com.udea.business.services;

import com.udea.business.entities.EstadoVisibleReserva;
import com.udea.business.entities.Reserva;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Determina el estado visible de las reservas y las agrupa en "Próximas" e "Historial" (HU-0019).
 */
public interface IClasificadorReservas {

    EstadoVisibleReserva estadoVisible(Reserva reserva, LocalDateTime ahora);

    ReservasClasificadas clasificar(List<Reserva> reservas, LocalDateTime ahora);

    /**
     * @param proximas  confirmadas que aún no inician, de la más cercana a la más lejana.
     * @param historial realizadas y canceladas, de la más reciente a la más antigua.
     */
    record ReservasClasificadas(List<Reserva> proximas, List<Reserva> historial) {
    }
}
