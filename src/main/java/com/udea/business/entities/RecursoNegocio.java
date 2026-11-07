package com.udea.business.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

/**
 * Vista de solo lectura de la tabla recurso (administrada por Masters).
 * Business la consulta para verificar existencia, disponibilidad y pertenencia al negocio
 * antes de asociar un recurso a un servicio.
 */
@Entity
@Immutable
@Table(name = "recurso")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecursoNegocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recurso")
    private Integer idRecurso;

    @Column(name = "negocio_id", nullable = false)
    private Integer negocioId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, length = 20)
    private String estado;
}
