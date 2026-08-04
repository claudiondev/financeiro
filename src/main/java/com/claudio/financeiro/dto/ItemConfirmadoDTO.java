package com.claudio.financeiro.dto;

import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.TipoTransacaoImportada;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma transação da tela de revisão que o usuário manteve marcada, enviada de volta em
 * POST /importacao/confirmar. Categoria é obrigatória só quando tipo = GASTO — validado
 * no ImportacaoService (Bean Validation não faz bem constraint condicional entre campos).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemConfirmadoDTO {

    @NotBlank
    private String fitid;

    @NotNull
    private LocalDate data;

    @NotNull
    @Positive
    private BigDecimal valor;

    private String descricao;

    @NotNull
    private TipoTransacaoImportada tipo;

    private CategoriaGasto categoria;
}
