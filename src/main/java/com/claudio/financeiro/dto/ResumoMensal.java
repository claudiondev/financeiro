package com.claudio.financeiro.dto;

import java.util.List;
import java.util.Map;

/** Resposta de GET /gastos/resumo: panorama financeiro para o dashboard. */
public class ResumoMensal {

    private Double totalSalario;
    private Double totalGasto;
    private Double saldo;
    private String mensagem;
    private Double maiorGasto;

    /** categoria → total gasto; usado pelo frontend para o gráfico de pizza. */
    private Map<String, Double> categorias;

    private List<TransacaoDTO> transacoesRecentes;

    public Double getTotalSalario() { return totalSalario; }
    public void setTotalSalario(Double totalSalario) { this.totalSalario = totalSalario; }

    public Double getTotalGasto() { return totalGasto; }
    public void setTotalGasto(Double totalGasto) { this.totalGasto = totalGasto; }

    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public Double getMaiorGasto() { return maiorGasto; }
    public void setMaiorGasto(Double maiorGasto) { this.maiorGasto = maiorGasto; }

    public Map<String, Double> getCategorias() { return categorias; }
    public void setCategorias(Map<String, Double> categorias) { this.categorias = categorias; }

    public List<TransacaoDTO> getTransacoesRecentes() { return transacoesRecentes; }
    public void setTransacoesRecentes(List<TransacaoDTO> transacoesRecentes) {
        this.transacoesRecentes = transacoesRecentes;
    }
}
