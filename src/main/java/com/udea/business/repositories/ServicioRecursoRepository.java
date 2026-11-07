package com.udea.business.repositories;

import com.udea.business.entities.ServicioRecurso;
import com.udea.business.entities.ServicioRecurso.ServicioRecursoId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioRecursoRepository extends JpaRepository<ServicioRecurso, ServicioRecursoId> {
    @Query("SELECT sr FROM ServicioRecurso sr "
            + "JOIN FETCH sr.recursoNegocio r "
            + "WHERE sr.id.servicioId = :servicioId")
    List<ServicioRecurso> findByServicioIdWithRecurso(@Param("servicioId") Integer servicioId);
}
