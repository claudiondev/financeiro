package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

}