package com.udea.business.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Vinculación entre un servicio del catálogo y un proveedor que lo presta.
 */
@Entity
@Table(
        name = "servicio_proveedor",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_servicio_proveedor",
                columnNames = {"servicio_id", "proveedor_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio_proveedor")
    private Integer idServicioProveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private PerfilProveedor proveedor;

    @Column(name = "fecha_asignacion", nullable = false)
    private OffsetDateTime fechaAsignacion;

    @PrePersist
    void alCrear() {
        if (fechaAsignacion == null) {
            fechaAsignacion = OffsetDateTime.now();
        }
    }
}
