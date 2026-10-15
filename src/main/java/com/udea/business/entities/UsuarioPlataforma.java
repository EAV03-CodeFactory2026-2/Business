package com.udea.business.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.util.HashSet;
import java.util.Set;

/**
 * Vista de solo lectura de la tabla usuario (la administra Masters).
 * Solo mapea las columnas que Business necesita para el directorio de proveedores.
 */
@Entity
@Immutable
@Table(name = "usuario")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioPlataforma {

    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 30)
    private String estado;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "asignacion_rol",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    @Builder.Default
    private Set<RolPlataforma> roles = new HashSet<>();

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
