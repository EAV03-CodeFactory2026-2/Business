package com.udea.business.repositories;

import com.udea.business.entities.Recurso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecursoRepository extends JpaRepository<Recurso, Integer> {
    List<Recurso> findByNegocioIdNegocioOrderByNombreAsc(Integer negocioId);
    boolean existsByNegocioIdNegocioAndNombreNormalizado(Integer negocioId, String nombreNormalizado);
}
