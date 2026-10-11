package com.udea.business.repositories;

import com.udea.business.entities.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Integer> {
    List<Servicio> findByNegocioIdNegocio(Integer negocioId);
    boolean existsByNegocioIdNegocioAndNombreIgnoreCase(Integer negocioId, String nombre);
}
