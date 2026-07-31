package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.CriarGastoFixoRequest;
import com.claudio.financeiro.dto.GastoFixoDTO;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.GastoFixoService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gastos-fixos")
public class GastoFixoController {

    private final GastoFixoService gastoFixoService;

    public GastoFixoController(GastoFixoService gastoFixoService) {
        this.gastoFixoService = gastoFixoService;
    }

    @PostMapping
    public GastoFixoDTO criar(@Valid @RequestBody CriarGastoFixoRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoFixoService.criar(request, usuarioLogado);
    }

    @GetMapping
    public List<GastoFixoDTO> listar(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoFixoService.listarComStatus(usuarioLogado.getId());
    }

    @PutMapping("/{id}")
    public GastoFixoDTO atualizar(@PathVariable Long id, @Valid @RequestBody CriarGastoFixoRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoFixoService.atualizar(id, request, usuarioLogado.getId());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        gastoFixoService.deletar(id, usuarioLogado.getId());
    }

    @PatchMapping("/{id}/pausar")
    public GastoFixoDTO pausar(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoFixoService.pausar(id, usuarioLogado.getId());
    }

    @PatchMapping("/{id}/reativar")
    public GastoFixoDTO reativar(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoFixoService.reativar(id, usuarioLogado.getId());
    }

    // Usado pelo aviso no Resumo e pelo disparo do e-mail de lembrete (efeito colateral do service).
    @GetMapping("/pendentes-alerta")
    public List<GastoFixoDTO> pendentesAlerta(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoFixoService.listarPendentesAlerta(usuarioLogado);
    }
}
