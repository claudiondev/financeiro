package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    List<Orcamento> findByUsuarioId(Long usuarioId);

    Optional<Orcamento> findByUsuarioIdAndCategoria(Long usuarioId, CategoriaGasto categoria);
}
