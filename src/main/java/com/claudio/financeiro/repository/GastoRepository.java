package com.claudio.financeiro.repository;

import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findByUsuarioId(Long usuarioId);

    @Query("SELECT g FROM Gasto g WHERE g.usuario.id = :usuarioId " +
            "AND (:categoria IS NULL OR g.categoria = :categoria) " +
            "AND (:mes IS NULL OR MONTH(g.data) = :mes) " +
            "AND (:ano IS NULL OR YEAR(g.data) = :ano)")
    List<Gasto> findByFiltros(
            @Param("usuarioId") Long usuarioId,
            @Param("categoria") CategoriaGasto categoria,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano
    );

    // Usados pela geração automática de gastos fixos (GastoFixoService) — checam se o
    // gasto do mês já existe antes de gerar de novo (idempotência).
    boolean existsByGastoFixoIdAndDataBetween(Long gastoFixoId, LocalDate inicio, LocalDate fim);

    // Lista (não Optional): em tese só existe um Gasto por GastoFixo+mês, mas uma corrida
    // concorrente em garantirGastosDoMesGerados já gerou duplicata em produção (ver V11) —
    // essa busca não pode quebrar se isso acontecer de novo. Ordenado por id para ser
    // determinístico sobre qual dos dois "vence" ao exibir o status.
    List<Gasto> findByGastoFixoIdAndDataBetweenOrderByIdAsc(Long gastoFixoId, LocalDate inicio, LocalDate fim);

    List<Gasto> findByUsuarioIdAndGrupoParcelamentoIsNotNull(Long usuarioId);

    List<Gasto> findByUsuarioIdAndGrupoParcelamento(Long usuarioId, String grupoParcelamento);

    @Query("SELECT YEAR(g.data), MONTH(g.data), SUM(g.valor) FROM Gasto g " +
           "WHERE g.usuario.id = :usuarioId AND g.pago = true " +
           "AND g.data >= :desde GROUP BY YEAR(g.data), MONTH(g.data)")
    List<Object[]> somarGastosPagosAgrupadoPorMes(@Param("usuarioId") Long usuarioId,
                                                   @Param("desde") LocalDate desde);
}
