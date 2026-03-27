package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.Salario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalarioRepository extends JpaRepository<Salario, Long> {

    // Essencial para o cálculo do resumo mensal individual
    List<Salario> findByUsuarioId(Long usuarioId);
}