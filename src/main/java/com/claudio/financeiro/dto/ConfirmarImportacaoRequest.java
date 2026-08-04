package com.claudio.financeiro.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Corpo de POST /importacao/confirmar — envelope pra @Valid cascatear pros itens da lista. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmarImportacaoRequest {

    @NotEmpty
    @Valid
    private List<ItemConfirmadoDTO> itens;
}
