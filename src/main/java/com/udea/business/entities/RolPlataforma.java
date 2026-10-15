package com.udea.business.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

/**
 * Vista de solo lectura de la tabla rol (la administra Masters).
 */
@Entity
@Immutable
@Table(name = "rol")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolPlataforma {

    public static final String PROVEEDOR = "Proveedor";

    @Id
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(name = "nombre_rol", nullable = false, length = 30)
    private String nombreRol;
}
