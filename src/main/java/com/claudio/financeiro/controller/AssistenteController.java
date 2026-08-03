package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.InsightDTO;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.GeradorDeInsight;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/assistente")
public class AssistenteController {

    private final GeradorDeInsight geradorDeInsight;

    public AssistenteController(GeradorDeInsight geradorDeInsight) {
        this.geradorDeInsight = geradorDeInsight;
    }

    @GetMapping("/insights")
    public List<InsightDTO> insights(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return geradorDeInsight.gerarInsights(usuarioLogado.getId());
    }
}
