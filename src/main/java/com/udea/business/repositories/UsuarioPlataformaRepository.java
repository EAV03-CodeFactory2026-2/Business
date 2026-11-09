package com.udea.business.repositories;

import com.udea.business.entities.UsuarioPlataforma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioPlataformaRepository extends JpaRepository<UsuarioPlataforma, Integer> {
}
