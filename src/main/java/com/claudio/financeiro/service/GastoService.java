package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.CriarGastoRequest;
import com.claudio.financeiro.dto.GastoDTO;
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

    public GastoService(GastoRepository gastoRepository, GastoFixoService gastoFixoService) {
        this.gastoRepository = gastoRepository;
        this.gastoFixoService = gastoFixoService;
    }

    @Transactional
    public GastoDTO criar(CriarGastoRequest request, Usuario usuarioLogado) {
        int parcelas = parcelasValidadas(request);

        if (parcelas == 1) {
            return toDTO(salvar(paraEntidade(request, usuarioLogado)));
        }

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

        return toDTO(criadas.get(0));
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

    public void deletar(Long id, Long usuarioId) {
        buscarComOwnership(id, usuarioId);
        gastoRepository.deleteById(id);
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
