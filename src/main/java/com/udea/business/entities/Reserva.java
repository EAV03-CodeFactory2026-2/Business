package com.udea.business.entities;

import com.udea.business.entities.converters.EstadoReservaConverter;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "reserva",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reserva_codigo", columnNames = {"codigo_reserva"}),
                @UniqueConstraint(name = "uk_reserva_id_estado", columnNames = {"id_reserva", "estado"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer idReserva;

    @Column(name = "codigo_reserva", nullable = false, length = 20, unique = true)
    private String codigoReserva;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(name = "proveedor_id", nullable = false)
    private Integer proveedorId;

    @Column(name = "servicio_id", nullable = false)
    private Integer servicioId;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "fin", nullable = false)
    private LocalDateTime fin;

    @Column(name = "notas", length = 250)
    private String notas;

    @Column(name = "nombre_servicio", nullable = false, length = 100)
    private String nombreServicio;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos;

    @Column(name = "precio_servicio", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioServicio;

    @Column(name = "moneda_codigo_iso", nullable = false, length = 3)
    private String monedaCodigoIso;

    @Convert(converter = EstadoReservaConverter.class)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoReserva estado = EstadoReserva.CONFIRMADA;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", insertable = false, updatable = false)
    private UsuarioPlataforma cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", insertable = false, updatable = false)
    private PerfilProveedor perfilProveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", insertable = false, updatable = false)
    private Servicio servicio;

    @PrePersist
    void alCrear() {
        if (fechaCreacion == null) {
            fechaCreacion = OffsetDateTime.now();
        }
        if (fechaActualizacion == null) {
            fechaActualizacion = OffsetDateTime.now();
        }
        if (estado == null) {
            estado = EstadoReserva.CONFIRMADA;
        }
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = OffsetDateTime.now();
    }
}
