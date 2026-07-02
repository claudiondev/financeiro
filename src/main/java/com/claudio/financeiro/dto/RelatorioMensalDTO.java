package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resposta de GET /gastos/relatorio: categorias com totais/percentuais/médias
 * do período, mais os totais de entradas (salários) e saídas (gastos).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioMensalDTO {

    private List<CategoriaDTO> categorias;
    private Double totalEntradas;
    private Double totalSaidas;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoriaDTO {

        private String nome;
        private Double valor;

        /** (valor / totalSaidas) * 100 */
        private Double percentual;

        // Por ora igual ao valor do período; no futuro pode virar média histórica por mês
        private Double media;
    }
}
