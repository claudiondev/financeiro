package com.claudio.financeiro.dto;

import java.util.UUID;

public record MensagemChatResposta(UUID requisicaoId, String resposta) {}
