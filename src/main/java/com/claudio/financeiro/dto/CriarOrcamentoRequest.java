package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Dados de entrada para criar/atualizar o limite mensal de uma categoria (upsert por categoria). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarOrcamentoRequest {

    @NotNull
    private CategoriaGasto categoria;

    @NotNull
    @Positive
    private BigDecimal limiteMensal;
}
