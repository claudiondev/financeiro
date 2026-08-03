package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.CriarOrcamentoRequest;
import com.claudio.financeiro.dto.InsightDTO;
import com.claudio.financeiro.dto.OrcamentoDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Orcamento;
import com.claudio.financeiro.model.Severidade;
import com.claudio.financeiro.model.StatusOrcamento;
import com.claudio.financeiro.model.TipoInsight;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.OrcamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrcamentoService {

    private static final BigDecimal LIMIAR_ATENCAO = BigDecimal.valueOf(80);
    private static final BigDecimal LIMIAR_ESTOURADO = BigDecimal.valueOf(100);

    private final OrcamentoRepository orcamentoRepository;
    private final GastoRepository gastoRepository;
    private final GastoFixoService gastoFixoService;

    public OrcamentoService(OrcamentoRepository orcamentoRepository, GastoRepository gastoRepository,
                            GastoFixoService gastoFixoService) {
        this.orcamentoRepository = orcamentoRepository;
        this.gastoRepository = gastoRepository;
        this.gastoFixoService = gastoFixoService;
    }

    // Upsert por categoria: usuário define o limite uma vez, atualizações seguintes só ajustam o valor
    public OrcamentoDTO salvarOuAtualizar(CriarOrcamentoRequest request, Usuario usuarioLogado) {
        Orcamento orcamento = orcamentoRepository
                .findByUsuarioIdAndCategoria(usuarioLogado.getId(), request.getCategoria())
                .orElseGet(Orcamento::new);

        orcamento.setCategoria(request.getCategoria());
        orcamento.setLimiteMensal(request.getLimiteMensal());
        orcamento.setUsuario(usuarioLogado);

        Orcamento salvo = orcamentoRepository.save(orcamento);
        return new OrcamentoDTO(salvo.getId(), salvo.getCategoria(), salvo.getLimiteMensal(), null, null, null);
    }

    public List<OrcamentoDTO> listarComConsumo(Long usuarioId, Integer mes, Integer ano) {
        // Chamado uma vez aqui (não dentro do loop por categoria) — evita gerar redundante.
        if (mes != null && ano != null) {
            gastoFixoService.garantirGastosDoMesGerados(usuarioId, mes, ano);
        }

        return orcamentoRepository.findByUsuarioId(usuarioId).stream()
                .map(orcamento -> toDTOComConsumo(orcamento, mes, ano))
                .collect(Collectors.toList());
    }

    // Verifica ownership antes de deletar, para evitar IDOR (usuário apagando meta de outro)
    public void deletar(Long id, Long usuarioId) {
        Orcamento orcamento = orcamentoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Orçamento não encontrado"));

        if (!orcamento.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        orcamentoRepository.deleteById(id);
    }

    /**
     * Soma os gastos pagos da categoria no período — reaproveitado pelo AssistenteService (Fase 6).
     * Conta fixa pendente não conta no consumo do orçamento ainda, só depois de paga.
     */
    public BigDecimal calcularConsumoDaCategoria(Long usuarioId, CategoriaGasto categoria, Integer mes, Integer ano) {
        return gastoRepository.findByFiltros(usuarioId, categoria, mes, ano).stream()
                .filter(Gasto::isPago)
                .map(Gasto::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Verifica se o gasto recém-criado levou a categoria a estourar ou chegar perto do limite —
     * usado por GastoService.criar() pra devolver um aviso não bloqueante (Fase 6). Retorna null
     * se não há orçamento definido pra essa categoria, ou se o consumo segue dentro do limite.
     */
    public InsightDTO avaliarAvisoDeEstouro(Long usuarioId, CategoriaGasto categoria, LocalDate data) {
        return orcamentoRepository.findByUsuarioIdAndCategoria(usuarioId, categoria)
                .map(orcamento -> criarAvisoSeNecessario(usuarioId, orcamento, data))
                .orElse(null);
    }

    private InsightDTO criarAvisoSeNecessario(Long usuarioId, Orcamento orcamento, LocalDate data) {
        BigDecimal consumido = calcularConsumoDaCategoria(usuarioId, orcamento.getCategoria(), data.getMonthValue(), data.getYear());
        BigDecimal percentual = calcularPercentual(consumido, orcamento.getLimiteMensal());
        StatusOrcamento status = statusDoConsumo(percentual);

        if (status == StatusOrcamento.DENTRO_DO_LIMITE) {
            return null;
        }

        boolean estourado = status == StatusOrcamento.ESTOURADO;
        return new InsightDTO(
                estourado ? TipoInsight.ORCAMENTO_ESTOURADO : TipoInsight.ORCAMENTO_ATENCAO,
                estourado ? Severidade.CRITICO : Severidade.ATENCAO,
                orcamento.getCategoria(),
                estourado ? "Orçamento estourado" : "Orçamento quase no limite",
                String.format("Esse gasto levou o orçamento de %s a %s%% do limite mensal.",
                        orcamento.getCategoria().getDescricao(), percentual.setScale(0, RoundingMode.HALF_UP))
        );
    }

    private OrcamentoDTO toDTOComConsumo(Orcamento orcamento, Integer mes, Integer ano) {
        BigDecimal consumido = calcularConsumoDaCategoria(
                orcamento.getUsuario().getId(), orcamento.getCategoria(), mes, ano
        );
        BigDecimal percentual = calcularPercentual(consumido, orcamento.getLimiteMensal());

        return new OrcamentoDTO(
                orcamento.getId(),
                orcamento.getCategoria(),
                orcamento.getLimiteMensal(),
                consumido,
                percentual,
                statusDoConsumo(percentual)
        );
    }

    private BigDecimal calcularPercentual(BigDecimal consumido, BigDecimal limite) {
        if (limite.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return consumido.divide(limite, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private StatusOrcamento statusDoConsumo(BigDecimal percentual) {
        if (percentual.compareTo(LIMIAR_ESTOURADO) >= 0) return StatusOrcamento.ESTOURADO;
        if (percentual.compareTo(LIMIAR_ATENCAO) >= 0) return StatusOrcamento.ATENCAO;
        return StatusOrcamento.DENTRO_DO_LIMITE;
    }
}
