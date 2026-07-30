package com.claudio.financeiro.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dados de entrada para atualizar o nome de exibição do usuário logado. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AtualizarPerfilRequest {

    @Size(max = 100)
    private String nome;
}
