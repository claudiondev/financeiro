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

/**
 * Controller de salários — gerencia as entradas de renda do usuário autenticado.
 *
 * Todos os métodos retornam SalarioDTO em vez da entidade Salario.
 * Isso protege o campo @ManyToOne Usuario de ser serializado na resposta,
 * evitando a exposição de dados internos do usuário (email, authorities, etc.).
 */
@RestController
@RequestMapping("/salario")
public class SalarioController {

    @Autowired
    private SalarioService salarioService;

    /**
     * Cria um novo registro de salário/renda e retorna o SalarioDTO.
     *
     * Fluxo: salvar entidade → converter para DTO → retornar ao cliente.
     * A conversão é delegada ao serviço (centralização do mapeamento).
     */
    @PostMapping
    public SalarioDTO criar(@Valid @RequestBody Salario salario, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        salario.setUsuario(usuarioLogado);
        Salario salvo = salarioService.salvar(salario);
        return salarioService.toDTO(salvo);
    }

    /**
     * Lista todos os salários do usuário como DTOs.
     *
     * A conversão entity → DTO é feita aqui no controller com stream().map(),
     * porque o método listarPorUsuario() do serviço retorna entidades (para
     * manter a compatibilidade com os testes unitários existentes).
     */
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
        // O serviço verifica ownership antes de deletar (proteção contra IDOR)
        salarioService.deletar(id, usuarioLogado.getId());
    }
}
