package com.claudio.financeiro.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Resposta de GET /gastos/resumo: panorama financeiro para o dashboard. */
public class ResumoMensal {

    private BigDecimal totalSalario;
    private BigDecimal totalGasto;
    private BigDecimal saldo;
    private String mensagem;
    private BigDecimal maiorGasto;

    /** categoria → total gasto; usado pelo frontend para o gráfico de pizza. */
    private Map<String, BigDecimal> categorias;

    private List<TransacaoDTO> transacoesRecentes;

    public BigDecimal getTotalSalario() { return totalSalario; }
    public void setTotalSalario(BigDecimal totalSalario) { this.totalSalario = totalSalario; }

    public BigDecimal getTotalGasto() { return totalGasto; }
    public void setTotalGasto(BigDecimal totalGasto) { this.totalGasto = totalGasto; }

    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public BigDecimal getMaiorGasto() { return maiorGasto; }
    public void setMaiorGasto(BigDecimal maiorGasto) { this.maiorGasto = maiorGasto; }

    public Map<String, BigDecimal> getCategorias() { return categorias; }
    public void setCategorias(Map<String, BigDecimal> categorias) { this.categorias = categorias; }

    public List<TransacaoDTO> getTransacoesRecentes() { return transacoesRecentes; }
    public void setTransacoesRecentes(List<TransacaoDTO> transacoesRecentes) {
        this.transacoesRecentes = transacoesRecentes;
    }
}
