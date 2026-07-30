package com.claudio.financeiro.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dados de entrada para criar um salário — evita expor a entidade JPA diretamente no @RequestBody. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarSalarioRequest {

    @NotNull
    @Positive
    private BigDecimal valor;

    private BigDecimal comissao;
    private BigDecimal adicional;
    private String descricao;

    @NotNull
    private LocalDate data;
}
