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

    /** HU-0019: reservas del cliente con servicio, negocio, proveedor y cancelación cargados en una sola consulta. */
    @Query("SELECT r FROM Reserva r "
            + "LEFT JOIN FETCH r.servicio s LEFT JOIN FETCH s.negocio "
            + "LEFT JOIN FETCH r.perfilProveedor p LEFT JOIN FETCH p.usuario "
            + "LEFT JOIN FETCH r.cancelacion "
            + "WHERE r.clienteId = :clienteId")
    List<Reserva> findDetalladasPorCliente(@Param("clienteId") Integer clienteId);

    /** HU-0019: detalle de una reserva solo si pertenece al cliente indicado. */
    @Query("SELECT r FROM Reserva r "
            + "LEFT JOIN FETCH r.servicio s LEFT JOIN FETCH s.negocio "
            + "LEFT JOIN FETCH r.perfilProveedor p LEFT JOIN FETCH p.usuario "
            + "LEFT JOIN FETCH r.cancelacion "
            + "WHERE r.idReserva = :idReserva AND r.clienteId = :clienteId")
    Optional<Reserva> findDetalladaPorIdYCliente(@Param("idReserva") Integer idReserva,
                                                 @Param("clienteId") Integer clienteId);
}
