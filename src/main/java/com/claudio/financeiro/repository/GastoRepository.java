package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    // Este método é essencial para filtrar os gastos pelo dono da conta
    List<Gasto> findByUsuarioId(Long usuarioId);
}