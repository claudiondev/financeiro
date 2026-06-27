package com.claudio.financeiro.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO de resposta do endpoint GET /gastos/resumo.
 *
 * Contém um panorama completo das finanças do usuário:
 * totais, saldo, maior gasto, distribuição por categoria
 * e as transações mais recentes para exibir no dashboard.
 *
 * Por que não usar Lombok @Data aqui?
 * A classe original não usava. Mantemos o padrão para consistência — misturar
 * Lombok em algumas classes e getters/setters manuais em outras dificulta
 * a leitura do projeto. Padronizar Lombok em todo o projeto ou em nenhum
 * é uma decisão de time.
 */
public class ResumoMensal {

    private Double totalSalario;
    private Double totalGasto;
    private Double saldo;
    private String mensagem;

    /**
     * Valor do maior gasto individual do usuário (não filtrado por mês).
     * Exibido como destaque no card "Maior Gasto" do dashboard.
     */
    private Double maiorGasto;

    /**
     * Mapa de categoria → total gasto nessa categoria.
     * Exemplo: {"Alimentação": 500.0, "Transporte": 150.0}
     *
     * Usado pelo frontend para montar o gráfico de pizza por categoria.
     * O Map serializa para um JSON object, que o frontend percorre com
     * Object.entries() para transformar em array de {name, value}.
     */
    private Map<String, Double> categorias;

    /**
     * Últimas 5 transações do usuário (gastos recentes), ordenadas por data
     * decrescente. Exibidas na seção "Transações Recentes" do dashboard.
     */
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
