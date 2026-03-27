package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.GastoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gastos")
public class GastoController {

    @Autowired
    private GastoService gastoService;

    @PostMapping
    public Gasto criar(@RequestBody Gasto gasto, Authentication authentication) {
        // Pega o usuário logado do Token e vincula ao gasto
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        gasto.setUsuario(usuarioLogado);
        return gastoService.salvar(gasto);
    }

    @GetMapping
    public List<Gasto> listar(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.listarPorUsuario(usuarioLogado.getId());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        gastoService.deletar(id);
    }

    @GetMapping("/resumo")
    public ResumoMensal resumo(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.calcularResumo(usuarioLogado.getId());
    }
}