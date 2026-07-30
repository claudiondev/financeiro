package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados de entrada para criar/atualizar um gasto fixo (o "molde" da conta recorrente). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarGastoFixoRequest {

    @NotNull
    private CategoriaGasto categoria;

    @NotNull
    @Positive
    private BigDecimal valor;

    @NotBlank
    private String descricao;

    @NotNull
    @Min(1)
    @Max(31)
    private Integer diaVencimento;

    @NotNull
    private LocalDate dataInicio;
}
