package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.InsightDTO;

import java.util.List;

/**
 * Abstração pro motor de insights do Assistente financeiro. Hoje só existe uma implementação
 * (AssistenteService, motor de regras determinístico), mas a interface existe pra permitir uma
 * futura integração com LLM sem alterar o controller nem o contrato da API.
 */
public interface GeradorDeInsight {
    List<InsightDTO> gerarInsights(Long usuarioId);
}
