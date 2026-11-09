package com.udea.business.repositories;

import com.udea.business.entities.EstadoReserva;
import com.udea.business.entities.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    boolean existsByCodigoReserva(String codigoReserva);

    Optional<Reserva> findByCodigoReserva(String codigoReserva);

    List<Reserva> findByClienteIdOrderByInicioDesc(Integer clienteId);

    @Query("SELECT r FROM Reserva r WHERE r.proveedorId = :proveedorId "
            + "AND r.estado = :estado "
            + "AND r.inicio < :fin AND r.fin > :inicio")
    List<Reserva> findOverlappingByProveedor(
            @Param("proveedorId") Integer proveedorId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estado") EstadoReserva estado);

    @Query("SELECT r FROM Reserva r WHERE r.clienteId = :clienteId "
            + "AND r.estado = :estado "
            + "AND r.inicio < :fin AND r.fin > :inicio")
    List<Reserva> findOverlappingByCliente(
            @Param("clienteId") Integer clienteId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estado") EstadoReserva estado);

    @Query("SELECT r FROM Reserva r "
            + "JOIN r.servicio s "
            + "WHERE s.negocio.idNegocio = :negocioId "
            + "AND r.estado = :estado "
            + "AND r.inicio < :fin AND r.fin > :inicio")
    List<Reserva> findOverlappingConfirmedByNegocio(
            @Param("negocioId") Integer negocioId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estado") EstadoReserva estado);

    @Query("SELECT r FROM Reserva r WHERE r.proveedorId = :proveedorId "
            + "AND r.estado = :estado "
            + "AND r.inicio < :finDia AND r.fin > :inicioDia")
    List<Reserva> findConfirmedByProveedorAndRango(
            @Param("proveedorId") Integer proveedorId,
            @Param("inicioDia") LocalDateTime inicioDia,
            @Param("finDia") LocalDateTime finDia,
            @Param("estado") EstadoReserva estado);
}
