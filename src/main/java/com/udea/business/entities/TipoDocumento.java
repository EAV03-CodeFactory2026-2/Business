package com.udea.business.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

/**
 * Vista de solo lectura de la tabla tipo_documento (la administra Masters).
 */
@Entity
@Immutable
@Table(name = "tipo_documento")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoDocumento {

    @Id
    @Column(name = "id_tipo_documento")
    private Integer idTipoDocumento;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 50)
    private String nombre;
}
