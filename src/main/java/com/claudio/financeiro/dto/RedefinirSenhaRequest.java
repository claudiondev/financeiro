package com.claudio.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedefinirSenhaRequest {

    @NotBlank
    private String codigo;

    @NotBlank
    @Size(min = 6)
    private String novaSenha;
}
