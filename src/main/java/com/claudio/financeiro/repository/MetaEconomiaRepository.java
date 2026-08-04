package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.MetaEconomia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetaEconomiaRepository extends JpaRepository<MetaEconomia, Long> {

    List<MetaEconomia> findByUsuarioId(Long usuarioId);
}
