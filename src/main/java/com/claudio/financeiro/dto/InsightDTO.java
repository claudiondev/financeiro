package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Severidade;
import com.claudio.financeiro.model.TipoInsight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Um insight do Assistente financeiro — orçamento estourado, categoria em alta, ritmo de gastos ou dica educacional. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsightDTO {

    private TipoInsight tipo;
    private Severidade severidade;

    /** Categoria relacionada ao insight; null quando não se aplica (ex.: ritmo diário, que é geral). */
    private CategoriaGasto categoria;

    private String titulo;
    private String mensagem;
}
