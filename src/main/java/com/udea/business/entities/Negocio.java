package com.udea.business.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

/**
 * Vista de solo lectura de la tabla negocio (administrada por Masters).
 * Business la consulta únicamente para verificar a qué propietario pertenece el negocio de un servicio.
 */
@Entity
@Immutable
@Table(name = "negocio")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_negocio")
    private Integer idNegocio;

    @Column(name = "propietario_id", nullable = false)
    private Integer propietarioId;

    @Column(name = "nombre_negocio", nullable = false, length = 250)
    private String nombreNegocio;
}

