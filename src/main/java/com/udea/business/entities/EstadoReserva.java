package com.udea.business.entities;

import lombok.Getter;

@Getter
public enum EstadoReserva {
    CONFIRMADA(1, "Confirmada"),
    CANCELADA(2, "Cancelada");

    private final Integer id;
    private final String nombre;

    EstadoReserva(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public static EstadoReserva desdeId(Integer id) {
        if (id == null) {
            return null;
        }
        for (EstadoReserva estado : values()) {
            if (estado.id.equals(id)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de reserva inválido: " + id);
    }

    public static EstadoReserva desdeNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        for (EstadoReserva estado : values()) {
            if (estado.nombre.equalsIgnoreCase(nombre) || estado.name().equalsIgnoreCase(nombre)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de reserva inválido: " + nombre);
    }
}
