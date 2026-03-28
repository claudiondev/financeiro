package com.claudio.financeiro.controller;

import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.SalarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/salario")
public class SalarioController {

    @Autowired
    private SalarioService salarioService;

    @PostMapping
    public Salario criar(@RequestBody Salario salario, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        salario.setUsuario(usuarioLogado);
        return salarioService.salvar(salario);
    }

    @GetMapping
    public List<Salario> listar(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return salarioService.listarPorUsuario(usuarioLogado.getId());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        salarioService.deletar(id);
    }
}