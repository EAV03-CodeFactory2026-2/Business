package com.udea.business.entities;

import com.udea.business.entities.converters.EstadoReservaConverter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cancelacion_reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelacionReserva implements Persistable<Integer> {

    @Id
    @Column(name = "reserva_id")
    private Integer reservaId;

    @Convert(converter = EstadoReservaConverter.class)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.CANCELADA;

    @Column(name = "cancelada_por", nullable = false)
    private Integer canceladaPor;

    @Column(name = "fecha_cancelacion", nullable = false)
    private OffsetDateTime fechaCancelacion;

    @Column(name = "motivo", length = 250)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelada_por", insertable = false, updatable = false)
    private UsuarioPlataforma usuarioCancelador;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id", insertable = false, updatable = false)
    private Reserva reserva;

    @Transient
    @Builder.Default
    private boolean nuevo = true;

    @Override
    public Integer getId() {
        return this.reservaId;
    }

    @Override
    public boolean isNew() {
        return this.nuevo;
    }

    @PrePersist
    void alCrear() {
        if (fechaCancelacion == null) {
            fechaCancelacion = OffsetDateTime.now();
        }
        if (estado == null) {
            estado = EstadoReserva.CANCELADA;
        }
    }

    @PostPersist
    @PostLoad
    void marcarNoNuevo() {
        this.nuevo = false;
    }
}
