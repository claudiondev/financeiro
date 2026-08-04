package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.Salario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SalarioRepository extends JpaRepository<Salario, Long> {

    List<Salario> findByUsuarioId(Long usuarioId);

    @Query("SELECT YEAR(s.data), MONTH(s.data), " +
           "SUM(s.valor + COALESCE(s.comissao, 0) + COALESCE(s.adicional, 0)) " +
           "FROM Salario s WHERE s.usuario.id = :usuarioId AND s.data >= :desde " +
           "GROUP BY YEAR(s.data), MONTH(s.data)")
    List<Object[]> somarRendaAgrupadaPorMes(@Param("usuarioId") Long usuarioId,
                                             @Param("desde") LocalDate desde);

    // Mesma finalidade de GastoRepository.findFitidsExistentes: dedup em lote na importação de extrato.
    @Query("SELECT s.fitid FROM Salario s WHERE s.usuario.id = :usuarioId AND s.fitid IN :fitids")
    List<String> findFitidsExistentes(@Param("usuarioId") Long usuarioId, @Param("fitids") List<String> fitids);
}