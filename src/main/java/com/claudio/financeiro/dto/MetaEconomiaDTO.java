package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.StatusMetaEconomia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resposta de GET /metas-economia: a meta + progresso calculado ao vivo. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetaEconomiaDTO {

    private Long id;
    private String nome;
    private BigDecimal valorAlvo;
    private LocalDate prazo;

    private BigDecimal valorAcumulado;

    /** (valorAcumulado / valorAlvo) * 100, sem limitar em 100 — dá pra passar da meta. */
    private BigDecimal percentualConcluido;

    private StatusMetaEconomia status;
}
