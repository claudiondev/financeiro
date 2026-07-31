package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.CriarSalarioRequest;
import com.claudio.financeiro.dto.SalarioDTO;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.SalarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/salario")
public class SalarioController {

    private final SalarioService salarioService;

    public SalarioController(SalarioService salarioService) {
        this.salarioService = salarioService;
    }

    @PostMapping
    public SalarioDTO criar(@Valid @RequestBody CriarSalarioRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return salarioService.criar(request, usuarioLogado);
    }

    @GetMapping
    public List<SalarioDTO> listar(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return salarioService.listarPorUsuario(usuarioLogado.getId());
    }

    @PutMapping("/{id}")
    public SalarioDTO atualizar(@PathVariable Long id, @Valid @RequestBody CriarSalarioRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return salarioService.atualizar(id, request, usuarioLogado.getId());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        salarioService.deletar(id, usuarioLogado.getId());
    }
}
