package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.CriarGastoRequest;
import com.claudio.financeiro.dto.EvolucaoMensalDTO;
import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.ParcelamentoDTO;
import com.claudio.financeiro.dto.RelatorioMensalDTO;
import com.claudio.financeiro.dto.ResumoMensal;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.EvolucaoService;
import com.claudio.financeiro.service.GastoService;
import com.claudio.financeiro.service.RelatorioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gastos")
public class GastoController {

    private final GastoService gastoService;
    private final RelatorioService relatorioService;
    private final EvolucaoService evolucaoService;

    public GastoController(GastoService gastoService, RelatorioService relatorioService, EvolucaoService evolucaoService) {
        this.gastoService = gastoService;
        this.relatorioService = relatorioService;
        this.evolucaoService = evolucaoService;
    }

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

    @PatchMapping("/{id}/pagar")
    public GastoDTO marcarComoPago(@PathVariable Long id, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.marcarComoPago(id, usuarioLogado.getId());
    }

    @GetMapping("/resumo")
    public ResumoMensal resumo(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return relatorioService.calcularResumo(usuarioLogado.getId());
    }

    @GetMapping("/categorias")
    public Map<String, BigDecimal> resumoCategoria(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return relatorioService.resumoPorCategoria(usuarioLogado.getId());
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
        return relatorioService.getRelatorio(usuarioLogado.getId(), mes, ano);
    }

    @GetMapping("/parcelamentos")
    public List<ParcelamentoDTO> parcelamentos(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return gastoService.listarParcelamentosEmAberto(usuarioLogado.getId());
    }

    @GetMapping("/evolucao")
    public List<EvolucaoMensalDTO> evolucao(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "6") Integer meses
    ) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return evolucaoService.getEvolucaoMensal(usuarioLogado.getId(), meses);
    }
}
