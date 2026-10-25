package com.udea.business.repositories;

import com.udea.business.entities.ServicioProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ServicioProveedorRepository extends JpaRepository<ServicioProveedor, Integer> {

    @Query("SELECT sp.proveedor.usuarioId FROM ServicioProveedor sp "
            + "WHERE sp.servicio.idServicio = :servicioId AND sp.proveedor.usuarioId IN :proveedorIds")
    List<Integer> buscarProveedoresYaVinculados(@Param("servicioId") Integer servicioId,
                                               @Param("proveedorIds") Collection<Integer> proveedorIds);

    long countByServicioIdServicio(Integer servicioId);

    @Query("SELECT sp FROM ServicioProveedor sp "
            + "JOIN FETCH sp.proveedor p "
            + "JOIN FETCH p.usuario u "
            + "JOIN FETCH p.tipoDocumento "
            + "WHERE sp.servicio.idServicio = :servicioId")
    List<ServicioProveedor> findByServicioIdConDetalle(@Param("servicioId") Integer servicioId);

    @Query("SELECT sp FROM ServicioProveedor sp "
            + "JOIN FETCH sp.servicio s "
            + "JOIN FETCH s.negocio n "
            + "WHERE sp.proveedor.usuarioId = :proveedorId")
    List<ServicioProveedor> findByProveedorIdConServicioYNegocio(@Param("proveedorId") Integer proveedorId);
}
