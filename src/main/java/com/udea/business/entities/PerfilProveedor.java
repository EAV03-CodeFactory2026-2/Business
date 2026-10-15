package com.udea.business.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

/**
 * Vista de solo lectura de la tabla perfil_proveedor (la administra Masters).
 * Su id es el id del usuario proveedor.
 */
@Entity
@Immutable
@Table(name = "perfil_proveedor")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerfilProveedor {

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "usuario_id")
    private UsuarioPlataforma usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 100)
    private String especialidad;

    /** Ej.: "Carlos Ruiz (CC 123456)". Sirve para distinguir homónimos en los mensajes. */
    public String getIdentificacion() {
        return usuario.getNombreCompleto() + " (" + tipoDocumento.getCodigo() + " " + numeroDocumento + ")";
    }
}
