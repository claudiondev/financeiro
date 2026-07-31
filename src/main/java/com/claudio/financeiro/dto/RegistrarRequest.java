package com.claudio.financeiro.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarRequest {

    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String senha;

    @Size(max = 100)
    private String nome;
}
