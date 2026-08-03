package com.claudio.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).{8,}$",
            message = "A senha deve ter pelo menos 8 caracteres, incluindo uma letra maiúscula e um número"
    )
    private String novaSenha;
}
