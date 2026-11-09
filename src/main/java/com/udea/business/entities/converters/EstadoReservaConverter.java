package com.udea.business.entities.converters;

import com.udea.business.entities.EstadoReserva;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Conversor JPA para mapear EstadoReserva enum con la columna de texto 'estado' de la BD.
 * Almacena el valor legible ("Confirmada", "Cancelada") directamente en la base de datos.
 */
@Converter(autoApply = true)
public class EstadoReservaConverter implements AttributeConverter<EstadoReserva, String> {

    @Override
    public String convertToDatabaseColumn(EstadoReserva attribute) {
        return attribute != null ? attribute.getNombre() : null;
    }

    @Override
    public EstadoReserva convertToEntityAttribute(String dbData) {
        return dbData != null ? EstadoReserva.desdeNombre(dbData) : null;
    }
}
