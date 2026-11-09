package com.udea.business.entities;

import java.text.Normalizer;
import java.time.DayOfWeek;

public enum DiaSemana {
    LUNES("lunes"),
    MARTES("martes"),
    MIERCOLES("miércoles"),
    JUEVES("jueves"),
    VIERNES("viernes"),
    SABADO("sábado"),
    DOMINGO("domingo");

    private final String nombre;

    DiaSemana(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public static DiaSemana desdeDayOfWeek(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return null;
        }
        return switch (dayOfWeek) {
            case MONDAY -> LUNES;
            case TUESDAY -> MARTES;
            case WEDNESDAY -> MIERCOLES;
            case THURSDAY -> JUEVES;
            case FRIDAY -> VIERNES;
            case SATURDAY -> SABADO;
            case SUNDAY -> DOMINGO;
        };
    }

    public static DiaSemana desdeTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        String limpio = Normalizer.normalize(texto.trim().toUpperCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return DiaSemana.valueOf(limpio);
    }
}
