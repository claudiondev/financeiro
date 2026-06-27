package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.RelatorioMensalDTO;
import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.GastoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller de gastos — gerencia as despesas do usuário autenticado.
 *
 * Todos os endpoints extraem o usuário do contexto de segurança via
 * Authentication.getPrincipal(). Isso garante que cada operação afeta
 * apenas os dados do usuário logado — nunca de outro usuário.
 */
@RestController
@RequestMapping("/gastos")
public class GastoController {

    @Autowired
    private GastoService gastoService;

    /**
     * Cria um novo gasto e retorna um GastoDTO (sem o objeto Usuario embutido).
     *
     * Por que retornar GastoDTO em vez de Gasto?
     * A entidade Gasto contém um @ManyToOne Usuario. Serializá-la retornaria
     * o objeto Usuario completo na resposta — expondo email, status de conta,
     * authorities, etc. O DTO "achata" a resposta, incluindo apenas usuarioId.
     */
    @PostMapping
    public GastoDTO criar(@Valid @RequestBody Gasto gasto, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        gasto.setUsuario(usuarioLogado);
        Gasto salvo = gastoService.salvar(gasto);
        // O serviço centraliza a conversão para DTO
        return gastoService.toDTO(salvo);
    }

    @GetMapping
    public List<GastoDTO> listar(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.listarPorUsuario(usuarioLogado.getId());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        // O serviço verifica ownership antes de deletar (proteção contra IDOR)
        gastoService.deletar(id, usuarioLogado.getId());
    }

    /**
     * Retorna o resumo financeiro completo: totais, saldo, maior gasto,
     * distribuição por categoria e transações recentes.
     */
    @GetMapping("/resumo")
    public ResumoMensal resumo(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.calcularResumo(usuarioLogado.getId());
    }

    /**
     * Retorna um Map simples de categoria → total.
     * Mantido para compatibilidade com outros consumidores.
     * Para a página de Relatórios, use GET /gastos/relatorio.
     */
    @GetMapping("/categorias")
    public Map<String, Double> resumoCategoria(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.resumoPorCategoria(usuarioLogado.getId());
    }

    /**
     * Filtra gastos por categoria, mês e ano (todos opcionais).
     * Usado pela página de Gastos para filtragem no servidor.
     */
    @GetMapping("/filtrar")
    public List<GastoDTO> filtrar(
            Authentication authentication,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano
    ) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.filtrarGastos(usuarioLogado.getId(), categoria, mes, ano);
    }

    /**
     * Retorna o relatório mensal estruturado para a página de Relatórios.
     *
     * Diferença em relação a GET /gastos/categorias:
     * Este endpoint retorna um DTO rico com lista de categorias incluindo
     * percentual e média, além dos totais de entradas vs saídas do período.
     * O endpoint /categorias retorna apenas um Map simples.
     *
     * @param mes mês (1-12), opcional — omitir retorna todos os meses
     * @param ano ano (ex: 2026), opcional — omitir retorna todos os anos
     */
    @GetMapping("/relatorio")
    public RelatorioMensalDTO relatorio(
            Authentication authentication,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano
    ) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.getRelatorio(usuarioLogado.getId(), mes, ano);
    }
}
