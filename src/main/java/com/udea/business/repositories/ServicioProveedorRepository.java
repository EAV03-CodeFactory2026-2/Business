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
}
