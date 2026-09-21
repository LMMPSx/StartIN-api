package com.api.backend.repository;

import com.api.backend.model.Startup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StartupRepository extends JpaRepository<Startup, Long> {
    List<Startup> findByUsuarioId(Long usuarioId);
    boolean existsByCnpj(String cnpj);
}