package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.Salario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalarioRepository extends  JpaRepository<Salario, Long>{
}
