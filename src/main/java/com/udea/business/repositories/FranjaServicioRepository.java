package com.udea.business.repositories;

import com.udea.business.entities.DiaSemana;
import com.udea.business.entities.FranjaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FranjaServicioRepository extends JpaRepository<FranjaServicio, Integer> {

    @Query("SELECT f FROM FranjaServicio f "
            + "JOIN FETCH f.servicio s "
            + "JOIN FETCH f.proveedor p "
            + "JOIN FETCH p.usuario u "
            + "WHERE f.servicio.idServicio = :servicioId "
            + "ORDER BY f.diaSemana, f.horaInicio ASC")
    List<FranjaServicio> findByServicioIdServicio(@Param("servicioId") Integer servicioId);

    @Query("SELECT f FROM FranjaServicio f "
            + "JOIN FETCH f.servicio s "
            + "JOIN FETCH f.proveedor p "
            + "JOIN FETCH p.usuario u "
            + "WHERE f.servicio.idServicio = :servicioId "
            + "AND f.diaSemana = :diaSemana "
            + "ORDER BY f.horaInicio ASC")
    List<FranjaServicio> findByServicioIdServicioAndDiaSemana(
            @Param("servicioId") Integer servicioId,
            @Param("diaSemana") DiaSemana diaSemana);

    @Query("SELECT f FROM FranjaServicio f "
            + "JOIN FETCH f.servicio s "
            + "JOIN FETCH f.proveedor p "
            + "JOIN FETCH p.usuario u "
            + "WHERE f.servicio.idServicio = :servicioId "
            + "AND f.proveedor.usuarioId = :proveedorId "
            + "AND f.diaSemana = :diaSemana "
            + "ORDER BY f.horaInicio ASC")
    List<FranjaServicio> findByServicioIdServicioAndProveedorUsuarioIdAndDiaSemana(
            @Param("servicioId") Integer servicioId,
            @Param("proveedorId") Integer proveedorId,
            @Param("diaSemana") DiaSemana diaSemana);

    @Query("SELECT f FROM FranjaServicio f "
            + "JOIN FETCH f.servicio s "
            + "JOIN FETCH f.proveedor p "
            + "JOIN FETCH p.usuario u "
            + "WHERE f.servicio.idServicio = :servicioId "
            + "AND f.proveedor.usuarioId = :proveedorId "
            + "AND f.diaSemana = :diaSemana "
            + "AND f.horaInicio = :horaInicio")
    Optional<FranjaServicio> findByServicioIdServicioAndProveedorUsuarioIdAndDiaSemanaAndHoraInicio(
            @Param("servicioId") Integer servicioId,
            @Param("proveedorId") Integer proveedorId,
            @Param("diaSemana") DiaSemana diaSemana,
            @Param("horaInicio") LocalTime horaInicio);
}
