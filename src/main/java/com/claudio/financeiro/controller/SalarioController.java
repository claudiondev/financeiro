package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.SalarioDTO;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.SalarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/salario")
public class SalarioController {

    @Autowired
    private SalarioService salarioService;

    @PostMapping
    public SalarioDTO criar(@Valid @RequestBody Salario salario, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        salario.setUsuario(usuarioLogado);
        Salario salvo = salarioService.salvar(salario);
        return salarioService.toDTO(salvo);
    }

    @GetMapping
    public List<SalarioDTO> listar(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return salarioService.listarPorUsuario(usuarioLogado.getId())
                .stream()
                .map(salarioService::toDTO)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        salarioService.deletar(id, usuarioLogado.getId());
    }
}
