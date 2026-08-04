package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.CriarMetaEconomiaRequest;
import com.claudio.financeiro.dto.MetaEconomiaDTO;
import com.claudio.financeiro.dto.RegistrarAporteRequest;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.MetaEconomia;
import com.claudio.financeiro.model.StatusMetaEconomia;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.MetaEconomiaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Meta de juntar dinheiro — o oposto de Orcamento (teto de gasto). Um aporte é só um Gasto
 * normal (categoria Poupança) vinculado à meta: conta pro saldo do mês como qualquer gasto,
 * e o progresso é sempre a soma ao vivo desses gastos, nunca um contador guardado à parte.
 */
@Service
public class MetaEconomiaService {

    private final MetaEconomiaRepository metaEconomiaRepository;
    private final GastoRepository gastoRepository;

    public MetaEconomiaService(MetaEconomiaRepository metaEconomiaRepository, GastoRepository gastoRepository) {
        this.metaEconomiaRepository = metaEconomiaRepository;
        this.gastoRepository = gastoRepository;
    }

    public MetaEconomiaDTO criar(CriarMetaEconomiaRequest request, Usuario usuarioLogado) {
        MetaEconomia meta = new MetaEconomia();
        meta.setNome(request.getNome());
        meta.setValorAlvo(request.getValorAlvo());
        meta.setPrazo(request.getPrazo());
        meta.setUsuario(usuarioLogado);
        return toDTOComProgresso(metaEconomiaRepository.save(meta));
    }

    public MetaEconomiaDTO atualizar(Long id, CriarMetaEconomiaRequest request, Long usuarioId) {
        MetaEconomia meta = buscarComOwnership(id, usuarioId);
        meta.setNome(request.getNome());
        meta.setValorAlvo(request.getValorAlvo());
        meta.setPrazo(request.getPrazo());
        return toDTOComProgresso(metaEconomiaRepository.save(meta));
    }

    public List<MetaEconomiaDTO> listarComProgresso(Long usuarioId) {
        return metaEconomiaRepository.findByUsuarioId(usuarioId).stream()
                .map(this::toDTOComProgresso)
                .collect(Collectors.toList());
    }

    // Histórico de gastos já feitos pra essa meta permanece (FK vira NULL — migração V16),
    // só o "molde" da meta em si é removido.
    public void deletar(Long id, Long usuarioId) {
        buscarComOwnership(id, usuarioId);
        metaEconomiaRepository.deleteById(id);
    }

    public MetaEconomiaDTO registrarAporte(Long id, RegistrarAporteRequest request, Usuario usuarioLogado) {
        MetaEconomia meta = buscarComOwnership(id, usuarioLogado.getId());

        Gasto aporte = new Gasto();
        aporte.setDescricao("Aporte: " + meta.getNome());
        aporte.setValor(request.getValor());
        aporte.setCategoria(CategoriaGasto.POUPANCA);
        aporte.setData(request.getData());
        aporte.setMetaEconomia(meta);
        aporte.setUsuario(usuarioLogado);
        gastoRepository.save(aporte);

        return toDTOComProgresso(meta);
    }

    private MetaEconomiaDTO toDTOComProgresso(MetaEconomia meta) {
        BigDecimal acumulado = gastoRepository.somarValorPorMetaEconomia(meta.getId());
        BigDecimal percentual = calcularPercentual(acumulado, meta.getValorAlvo());

        return new MetaEconomiaDTO(
                meta.getId(),
                meta.getNome(),
                meta.getValorAlvo(),
                meta.getPrazo(),
                acumulado,
                percentual,
                calcularStatus(acumulado, meta)
        );
    }

    private BigDecimal calcularPercentual(BigDecimal acumulado, BigDecimal alvo) {
        if (alvo.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return acumulado.divide(alvo, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private StatusMetaEconomia calcularStatus(BigDecimal acumulado, MetaEconomia meta) {
        if (acumulado.compareTo(meta.getValorAlvo()) >= 0) {
            return StatusMetaEconomia.CONCLUIDA;
        }
        if (meta.getPrazo() != null && meta.getPrazo().isBefore(LocalDate.now())) {
            return StatusMetaEconomia.ATRASADA;
        }
        return StatusMetaEconomia.EM_ANDAMENTO;
    }

    private MetaEconomia buscarComOwnership(Long id, Long usuarioId) {
        MetaEconomia meta = metaEconomiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Meta não encontrada"));

        if (!meta.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return meta;
    }
}
