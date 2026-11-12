package com.udea.business.entities;

import lombok.Getter;

/**
 * Estado de una reserva tal como lo ve el usuario.
 * <p>
 * A diferencia de {@link EstadoReserva} (lo que se persiste), "Realizada" no se guarda:
 * se deriva de una reserva confirmada cuya hora de inicio ya pasó.
 */
@Getter
public enum EstadoVisibleReserva {
    CONFIRMADA("Confirmada"),
    REALIZADA("Realizada"),
    CANCELADA("Cancelada");

    private final String nombre;

    EstadoVisibleReserva(String nombre) {
        this.nombre = nombre;
    }

    public boolean esProxima() {
        return this == CONFIRMADA;
    }
}
