package com.udea.business.repositories;

import com.udea.business.entities.PerfilProveedor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Lectura de proveedores (tablas de Masters) solo para validar los ids que se asignan.
 * La búsqueda en el directorio está en Masters.
 */
@Repository
public interface PerfilProveedorRepository extends JpaRepository<PerfilProveedor, Integer> {

    @Query("SELECT p FROM PerfilProveedor p JOIN FETCH p.usuario u JOIN FETCH p.tipoDocumento "
            + "WHERE p.usuarioId IN :ids AND u.estado = 'Activo' "
            + "AND EXISTS (SELECT r FROM u.roles r WHERE r.nombreRol = :rol)")
    List<PerfilProveedor> buscarActivosPorIds(@Param("ids") Collection<Integer> ids, @Param("rol") String rol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PerfilProveedor p WHERE p.usuarioId = :proveedorId")
    Optional<PerfilProveedor> findByIdConBloqueo(@Param("proveedorId") Integer proveedorId);
}
