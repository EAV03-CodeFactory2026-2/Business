package com.udea.business.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "recurso",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recurso_negocio_nombre_normalizado",
                columnNames = {"negocio_id", "nombre_normalizado"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recurso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recurso")
    private Integer idRecurso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @Column(nullable = false, length = 100)
    private String nombre;

    // Nombre sin tildes, en minúsculas y con espacios colapsados; garantiza la unicidad por negocio.
    @Column(name = "nombre_normalizado", nullable = false, length = 100)
    private String nombreNormalizado;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "Activo";
}
