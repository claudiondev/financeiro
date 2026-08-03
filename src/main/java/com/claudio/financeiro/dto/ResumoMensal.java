package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Resposta de GET /gastos/resumo: panorama financeiro para o dashboard. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumoMensal {

    private BigDecimal totalSalario;
    private BigDecimal totalGasto;
    private BigDecimal saldo;
    private String mensagem;
    private BigDecimal maiorGasto;

    /** categoria → total gasto; usado pelo frontend para o gráfico de pizza. */
    private Map<String, BigDecimal> categorias;

    private List<TransacaoDTO> transacoesRecentes;
}
