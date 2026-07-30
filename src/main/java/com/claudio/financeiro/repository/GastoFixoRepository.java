package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.GastoFixo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoFixoRepository extends JpaRepository<GastoFixo, Long> {

    List<GastoFixo> findByUsuarioId(Long usuarioId);

    List<GastoFixo> findByUsuarioIdAndAtivoTrue(Long usuarioId);
}
