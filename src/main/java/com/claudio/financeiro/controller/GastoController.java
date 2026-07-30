package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.CriarGastoRequest;
import com.claudio.financeiro.dto.EvolucaoMensalDTO;
import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.RelatorioMensalDTO;
import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.GastoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gastos")
public class GastoController {

    @Autowired
    private GastoService gastoService;

    @PostMapping
    public GastoDTO criar(@Valid @RequestBody CriarGastoRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.criar(request, usuarioLogado);
    }

    @GetMapping
    public List<GastoDTO> listar(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.listarPorUsuario(usuarioLogado.getId());
    }

    @PutMapping("/{id}")
    public GastoDTO atualizar(@PathVariable Long id, @Valid @RequestBody CriarGastoRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.atualizar(id, request, usuarioLogado.getId());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        gastoService.deletar(id, usuarioLogado.getId());
    }

    @GetMapping("/resumo")
    public ResumoMensal resumo(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.calcularResumo(usuarioLogado.getId());
    }

    // Mantido para compatibilidade com outros consumidores; a página de Relatórios usa /relatorio
    @GetMapping("/categorias")
    public Map<String, BigDecimal> resumoCategoria(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.resumoPorCategoria(usuarioLogado.getId());
    }

    @GetMapping("/filtrar")
    public List<GastoDTO> filtrar(
            Authentication authentication,
            @RequestParam(required = false) CategoriaGasto categoria,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano
    ) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.filtrarGastos(usuarioLogado.getId(), categoria, mes, ano);
    }

    @GetMapping("/relatorio")
    public RelatorioMensalDTO relatorio(
            Authentication authentication,
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano
    ) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.getRelatorio(usuarioLogado.getId(), mes, ano);
    }

    @GetMapping("/evolucao")
    public List<EvolucaoMensalDTO> evolucao(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "6") Integer meses
    ) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.getEvolucaoMensal(usuarioLogado.getId(), meses);
    }
}
