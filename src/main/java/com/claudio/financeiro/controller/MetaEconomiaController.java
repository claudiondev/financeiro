package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.CriarMetaEconomiaRequest;
import com.claudio.financeiro.dto.MetaEconomiaDTO;
import com.claudio.financeiro.dto.RegistrarAporteRequest;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.MetaEconomiaService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/metas-economia")
public class MetaEconomiaController {

    private final MetaEconomiaService metaEconomiaService;

    public MetaEconomiaController(MetaEconomiaService metaEconomiaService) {
        this.metaEconomiaService = metaEconomiaService;
    }

    @PostMapping
    public MetaEconomiaDTO criar(@Valid @RequestBody CriarMetaEconomiaRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return metaEconomiaService.criar(request, usuarioLogado);
    }

    @GetMapping
    public List<MetaEconomiaDTO> listar(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return metaEconomiaService.listarComProgresso(usuarioLogado.getId());
    }

    @PutMapping("/{id}")
    public MetaEconomiaDTO atualizar(@PathVariable Long id, @Valid @RequestBody CriarMetaEconomiaRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return metaEconomiaService.atualizar(id, request, usuarioLogado.getId());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        metaEconomiaService.deletar(id, usuarioLogado.getId());
    }

    @PostMapping("/{id}/aportes")
    public MetaEconomiaDTO registrarAporte(@PathVariable Long id, @Valid @RequestBody RegistrarAporteRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return metaEconomiaService.registrarAporte(id, request, usuarioLogado);
    }
}
