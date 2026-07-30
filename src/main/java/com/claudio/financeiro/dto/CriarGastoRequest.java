package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados de entrada para criar um gasto — evita expor a entidade JPA diretamente no @RequestBody. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarGastoRequest {

    @NotBlank
    private String descricao;

    @NotNull
    @Positive
    private BigDecimal valor;

    @NotNull
    private CategoriaGasto categoria;

    @NotNull
    private LocalDate data;
}
