package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.CriarOrcamentoRequest;
import com.claudio.financeiro.dto.OrcamentoDTO;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.OrcamentoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/orcamentos")
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @PostMapping
    public OrcamentoDTO salvar(@Valid @RequestBody CriarOrcamentoRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return orcamentoService.salvarOuAtualizar(request, usuarioLogado);
    }

    @GetMapping
    public List<OrcamentoDTO> listar(
            Authentication authentication,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano
    ) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        // Sem mes/ano informados, assume o mês corrente — a tela de Metas é sempre sobre "agora"
        int mesResolvido = mes != null ? mes : LocalDate.now().getMonthValue();
        int anoResolvido = ano != null ? ano : LocalDate.now().getYear();
        return orcamentoService.listarComConsumo(usuarioLogado.getId(), mesResolvido, anoResolvido);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        orcamentoService.deletar(id, usuarioLogado.getId());
    }
}
