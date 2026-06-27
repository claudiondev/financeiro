package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de resposta do endpoint GET /gastos/relatorio.
 *
 * Consolida em uma única resposta os dados que a página de Relatórios precisa:
 *   - categorias: lista com totais, percentuais e médias por categoria de gasto
 *   - totalEntradas: soma dos salários no período
 *   - totalSaidas: soma dos gastos no período
 *
 * Por que uma classe com classe interna estática?
 * CategoriaDTO só faz sentido no contexto de RelatorioMensalDTO — ela nunca
 * será usada sozinha. Manter como classe interna evita criar um arquivo extra
 * e torna a relação entre as classes explícita pelo próprio código.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioMensalDTO {

    private List<CategoriaDTO> categorias;
    private Double totalEntradas;
    private Double totalSaidas;

    /**
     * Resumo financeiro de uma única categoria de gasto.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaDTO {

        /** Nome da categoria (ex: "Alimentação", "Transporte"). */
        private String nome;

        /** Total gasto nessa categoria no período selecionado. */
        private Double valor;

        /**
         * Participação percentual em relação ao total de gastos do período.
         * Calculado como: (valor / totalSaidas) * 100.
         */
        private Double percentual;

        /**
         * Média mensal da categoria.
         * Por ora equivale ao valor do período selecionado.
         * Em versões futuras pode ser calculada dividindo o total histórico
         * pelo número de meses em que houve gasto nessa categoria.
         */
        private Double media;
    }
}
