package com.udea.business.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Asociación entre un servicio y los recursos que requiere para prestarse (HU-0045).
 * La clave primaria compuesta (servicio_id, recurso_id) garantiza la unicidad.
 * El campo negocioId está presente en la tabla para validar la consistencia del negocio.
 */
@Entity
@Table(name = "servicio_recurso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioRecurso {

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServicioRecursoId implements Serializable {

        @Column(name = "servicio_id")
        private Integer servicioId;

        @Column(name = "recurso_id")
        private Integer recursoId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ServicioRecursoId that)) return false;
            return Objects.equals(servicioId, that.servicioId)
                    && Objects.equals(recursoId, that.recursoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(servicioId, recursoId);
        }
    }

    @EmbeddedId
    private ServicioRecursoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("servicioId")
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("recursoId")
    @JoinColumn(name = "recurso_id", nullable = false)
    private RecursoNegocio recursoNegocio;

    @Column(name = "negocio_id", nullable = false)
    private Integer negocioId;

    @Column(name = "cantidad_requerida", nullable = false)
    private Short cantidadRequerida;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (fechaCreacion == null) {
            fechaCreacion = ahora;
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = ahora;
        }
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = OffsetDateTime.now();
    }
}
