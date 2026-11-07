package com.udea.business.repositories;

import com.udea.business.entities.RecursoNegocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecursoNegocioRepository extends JpaRepository<RecursoNegocio, Integer> {
    Optional<RecursoNegocio> findByIdRecursoAndNegocioId(Integer idRecurso, Integer negocioId);
}
