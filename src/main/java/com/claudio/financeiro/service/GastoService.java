package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.CriarGastoRequest;
import com.claudio.financeiro.dto.CriarGastoResponse;
import com.claudio.financeiro.dto.GastoDTO;
import com.claudio.financeiro.dto.InsightDTO;
import com.claudio.financeiro.dto.ParcelamentoDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.FormaPagamento;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GastoService {

    private final GastoRepository gastoRepository;
    private final GastoFixoService gastoFixoService;
    private final OrcamentoService orcamentoService;

    public GastoService(GastoRepository gastoRepository, GastoFixoService gastoFixoService, OrcamentoService orcamentoService) {
        this.gastoRepository = gastoRepository;
        this.gastoFixoService = gastoFixoService;
        this.orcamentoService = orcamentoService;
    }

    @Transactional
    public CriarGastoResponse criar(CriarGastoRequest request, Usuario usuarioLogado) {
        int parcelas = parcelasValidadas(request);
        Gasto principal;

        if (parcelas == 1) {
            principal = salvar(paraEntidade(request, usuarioLogado));
        } else {
            String grupo = UUID.randomUUID().toString();
            List<BigDecimal> valores = dividirEmParcelas(request.getValor(), parcelas);
            List<Gasto> criadas = new ArrayList<>();

            for (int i = 0; i < parcelas; i++) {
                Gasto parcela = paraEntidade(request, usuarioLogado);
                parcela.setValor(valores.get(i));
                parcela.setData(request.getData().plusMonths(i));
                parcela.setGrupoParcelamento(grupo);
                parcela.setNumeroParcela(i + 1);
                parcela.setTotalParcelas(parcelas);
                criadas.add(salvar(parcela));
            }
            principal = criadas.get(0);
        }

        InsightDTO aviso = orcamentoService.avaliarAvisoDeEstouro(usuarioLogado.getId(), principal.getCategoria(), principal.getData());
        return new CriarGastoResponse(toDTO(principal), aviso);
    }

    public List<ParcelamentoDTO> listarParcelamentosEmAberto(Long usuarioId) {
        LocalDate inicioDoMesAtual = YearMonth.now().atDay(1);

        return gastoRepository.findByUsuarioIdAndGrupoParcelamentoIsNotNull(usuarioId).stream()
                .collect(Collectors.groupingBy(Gasto::getGrupoParcelamento))
                .values().stream()
                .map(parcelas -> montarParcelamento(parcelas, inicioDoMesAtual))
                .filter(dto -> dto.getValorRestante().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(ParcelamentoDTO::getUltimaParcela))
                .collect(Collectors.toList());
    }

    public GastoDTO atualizar(Long id, CriarGastoRequest request, Long usuarioId) {
        Gasto gasto = buscarComOwnership(id, usuarioId);
        gasto.setDescricao(request.getDescricao());
        gasto.setValor(request.getValor());
        gasto.setCategoria(request.getCategoria());
        gasto.setData(request.getData());
        gasto.setFormaPagamento(request.getFormaPagamento());
        return toDTO(salvar(gasto));
    }

    private Gasto salvar(Gasto gasto) {
        return gastoRepository.save(gasto);
    }

    public List<GastoDTO> listarPorUsuario(Long usuarioId) {
        return gastoRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Uma parcela isolada não pode ser apagada — deixaria totalParcelas/numeroParcela das
    // demais inconsistente (ex.: "3/5" sem a parcela 2 nunca ter existido). Quem quer remover
    // a compra parcelada usa deletarParcelamento, que apaga o grupo inteiro de uma vez.
    public void deletar(Long id, Long usuarioId) {
        Gasto gasto = buscarComOwnership(id, usuarioId);
        if (gasto.getGrupoParcelamento() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Esse gasto faz parte de uma compra parcelada — para removê-lo, delete a compra inteira");
        }
        gastoRepository.deleteById(id);
    }

    @Transactional
    public void deletarParcelamento(Long id, Long usuarioId) {
        Gasto gasto = buscarComOwnership(id, usuarioId);
        if (gasto.getGrupoParcelamento() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esse gasto não faz parte de uma compra parcelada");
        }
        gastoRepository.deleteAll(
                gastoRepository.findByUsuarioIdAndGrupoParcelamento(usuarioId, gasto.getGrupoParcelamento())
        );
    }

    public List<GastoDTO> filtrarGastos(Long usuarioId, CategoriaGasto categoria, Integer mes, Integer ano) {
        if (mes != null && ano != null) {
            gastoFixoService.garantirGastosDoMesGerados(usuarioId, mes, ano);
        }

        return gastoRepository.findByFiltros(usuarioId, categoria, mes, ano)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public GastoDTO marcarComoPago(Long id, Long usuarioId) {
        Gasto gasto = buscarComOwnership(id, usuarioId);
        gasto.setPago(true);
        return toDTO(salvar(gasto));
    }

    public GastoDTO toDTO(Gasto gasto) {
        GastoDTO dto = new GastoDTO();
        dto.setId(gasto.getId());
        dto.setDescricao(gasto.getDescricao());
        dto.setValor(gasto.getValor());
        dto.setCategoria(gasto.getCategoria());
        dto.setData(gasto.getData());
        dto.setUsuarioId(gasto.getUsuario().getId());
        dto.setPago(gasto.isPago());
        dto.setGastoFixoId(gasto.getGastoFixo() != null ? gasto.getGastoFixo().getId() : null);
        dto.setFormaPagamento(gasto.getFormaPagamento());
        dto.setNumeroParcela(gasto.getNumeroParcela());
        dto.setTotalParcelas(gasto.getTotalParcelas());
        return dto;
    }

    private Gasto paraEntidade(CriarGastoRequest request, Usuario usuarioLogado) {
        Gasto gasto = new Gasto();
        gasto.setDescricao(request.getDescricao());
        gasto.setValor(request.getValor());
        gasto.setCategoria(request.getCategoria());
        gasto.setData(request.getData());
        gasto.setFormaPagamento(request.getFormaPagamento());
        gasto.setUsuario(usuarioLogado);
        return gasto;
    }

    private int parcelasValidadas(CriarGastoRequest request) {
        Integer parcelas = request.getTotalParcelas();
        if (parcelas == null || parcelas <= 1) {
            return 1;
        }

        if (request.getFormaPagamento() != FormaPagamento.CARTAO_CREDITO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Parcelamento só é permitido no cartão de crédito"
            );
        }

        return parcelas;
    }

    private List<BigDecimal> dividirEmParcelas(BigDecimal total, int parcelas) {
        BigDecimal valorParcela = total.divide(BigDecimal.valueOf(parcelas), 2, RoundingMode.HALF_UP);

        List<BigDecimal> valores = new ArrayList<>();
        for (int i = 0; i < parcelas - 1; i++) {
            valores.add(valorParcela);
        }
        valores.add(total.subtract(valorParcela.multiply(BigDecimal.valueOf(parcelas - 1L))));

        return valores;
    }

    private ParcelamentoDTO montarParcelamento(List<Gasto> parcelas, LocalDate inicioDoMesAtual) {
        Gasto referencia = parcelas.get(0);

        BigDecimal valorTotal = CalculoFinanceiroUtil.somarGastos(parcelas);
        List<Gasto> aVencer = parcelas.stream()
                .filter(g -> !g.getData().isBefore(inicioDoMesAtual))
                .collect(Collectors.toList());

        LocalDate ultimaParcela = parcelas.stream()
                .map(Gasto::getData)
                .max(Comparator.naturalOrder())
                .orElse(referencia.getData());

        return new ParcelamentoDTO(
                referencia.getGrupoParcelamento(),
                referencia.getDescricao(),
                referencia.getCategoria(),
                valorTotal,
                CalculoFinanceiroUtil.somarGastos(aVencer),
                parcelas.size() - aVencer.size(),
                referencia.getTotalParcelas(),
                ultimaParcela
        );
    }

    private Gasto buscarComOwnership(Long id, Long usuarioId) {
        Gasto gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gasto não encontrado"));

        if (!gasto.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return gasto;
    }
}
