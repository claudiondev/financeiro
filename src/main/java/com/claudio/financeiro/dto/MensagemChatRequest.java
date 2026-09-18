package com.claudio.financeiro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MensagemChatRequest(
        @NotBlank @Size(max = 1000) String mensagem,
        @NotNull UUID requisicaoId
) {}
