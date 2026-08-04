package com.claudio.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriarMetaEconomiaRequest {

    @NotBlank
    private String nome;

    @NotNull
    @Positive
    private BigDecimal valorAlvo;

    // Opcional — meta sem prazo definido.
    private LocalDate prazo;
}
