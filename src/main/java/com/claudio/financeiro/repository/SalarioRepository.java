package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.Salario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalarioRepository extends JpaRepository<Salario, Long> {

    List<Salario> findByUsuarioId(Long usuarioId);
}