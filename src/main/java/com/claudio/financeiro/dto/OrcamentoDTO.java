package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Resposta de GET /orcamentos: limite definido + quanto já foi consumido no período. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrcamentoDTO {

    private Long id;
    private CategoriaGasto categoria;
    private BigDecimal limiteMensal;
    private BigDecimal valorConsumido;

    /** (valorConsumido / limiteMensal) * 100 */
    private BigDecimal percentualConsumido;

    /** DENTRO_DO_LIMITE, ATENCAO (>=80%) ou ESTOURADO (>=100%) */
    private String status;
}
