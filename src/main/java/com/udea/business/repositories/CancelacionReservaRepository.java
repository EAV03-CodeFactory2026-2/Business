package com.udea.business.repositories;

import com.udea.business.entities.CancelacionReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CancelacionReservaRepository extends JpaRepository<CancelacionReserva, Integer> {

    Optional<CancelacionReserva> findByReservaId(Integer reservaId);
}
