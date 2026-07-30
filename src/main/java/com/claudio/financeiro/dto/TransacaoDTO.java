package com.claudio.financeiro.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Formato unificado de gastos e salários para a lista de transações recentes do dashboard. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransacaoDTO {

    private Long id;
    private String descricao;

    /** Categoria do gasto; null para entradas de salário. */
    private String categoria;

    private LocalDate data;
    private BigDecimal valor;

    /** "entrada" ou "saida" — usado pelo frontend para colorir a exibição. */
    private String tipo;
}
