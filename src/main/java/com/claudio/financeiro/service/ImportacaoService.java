package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.ImportacaoResultadoDTO;
import com.claudio.financeiro.dto.ItemConfirmadoDTO;
import com.claudio.financeiro.dto.TransacaoImportadaDTO;
import com.claudio.financeiro.model.CategoriaGasto;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.TipoTransacaoImportada;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoRepository;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Importação de extrato bancário (OFX) em lote — a alternativa viável a Open Finance
 * (descartado por exigir credenciamento bancário). Fluxo em 2 passos, sem tabela de
 * staging: processarArquivo só lê e devolve pro navegador revisar; nada é salvo até
 * confirmar() ser chamado com a lista já revisada.
 */
@Service
public class ImportacaoService {

    private final OfxParser ofxParser;
    private final GastoRepository gastoRepository;
    private final SalarioRepository salarioRepository;

    public ImportacaoService(OfxParser ofxParser, GastoRepository gastoRepository, SalarioRepository salarioRepository) {
        this.ofxParser = ofxParser;
        this.gastoRepository = gastoRepository;
        this.salarioRepository = salarioRepository;
    }

    public List<TransacaoImportadaDTO> processarArquivo(MultipartFile arquivo, Long usuarioId) {
        List<TransacaoOfx> transacoes = ofxParser.parse(lerConteudo(arquivo));

        List<String> fitids = transacoes.stream().map(TransacaoOfx::getFitid).collect(Collectors.toList());
        Set<String> jaImportados = new HashSet<>();
        jaImportados.addAll(gastoRepository.findFitidsExistentes(usuarioId, fitids));
        jaImportados.addAll(salarioRepository.findFitidsExistentes(usuarioId, fitids));

        return transacoes.stream().map(t -> paraDTO(t, jaImportados)).collect(Collectors.toList());
    }

    @Transactional
    public ImportacaoResultadoDTO confirmar(List<ItemConfirmadoDTO> itens, Usuario usuarioLogado) {
        int gastosCriados = 0;
        int salariosCriados = 0;

        for (ItemConfirmadoDTO item : itens) {
            if (item.getTipo() == TipoTransacaoImportada.GASTO && item.getCategoria() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Categoria é obrigatória para transações do tipo GASTO (fitid: " + item.getFitid() + ")");
            }

            try {
                if (item.getTipo() == TipoTransacaoImportada.GASTO) {
                    gastoRepository.save(paraGasto(item, usuarioLogado));
                    gastosCriados++;
                } else {
                    salarioRepository.save(paraSalario(item, usuarioLogado));
                    salariosCriados++;
                }
            } catch (DataIntegrityViolationException e) {
                // Mesma transação confirmada duas vezes (ex.: duplo clique, ou já tinha sido
                // importada entre a revisão e a confirmação) — a constraint única (V15) barra
                // no banco; aqui só ignoramos, o resultado final já reflete o que foi criado.
            }
        }

        return new ImportacaoResultadoDTO(gastosCriados, salariosCriados);
    }

    private String lerConteudo(MultipartFile arquivo) {
        try {
            return new String(arquivo.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado");
        }
    }

    private TransacaoImportadaDTO paraDTO(TransacaoOfx t, Set<String> jaImportados) {
        boolean ehGasto = t.getValor().signum() < 0;
        return new TransacaoImportadaDTO(
                t.getFitid(),
                t.getData(),
                t.getValor().abs(),
                t.getDescricao(),
                ehGasto ? TipoTransacaoImportada.GASTO : TipoTransacaoImportada.SALARIO,
                ehGasto ? CategoriaGasto.OUTROS : null,
                jaImportados.contains(t.getFitid())
        );
    }

    private Gasto paraGasto(ItemConfirmadoDTO item, Usuario usuarioLogado) {
        Gasto gasto = new Gasto();
        gasto.setDescricao(item.getDescricao());
        gasto.setValor(item.getValor());
        gasto.setCategoria(item.getCategoria());
        gasto.setData(item.getData());
        gasto.setFitid(item.getFitid());
        gasto.setUsuario(usuarioLogado);
        return gasto;
    }

    private Salario paraSalario(ItemConfirmadoDTO item, Usuario usuarioLogado) {
        Salario salario = new Salario();
        salario.setDescricao(item.getDescricao());
        salario.setValor(item.getValor());
        salario.setData(item.getData());
        salario.setFitid(item.getFitid());
        salario.setUsuario(usuarioLogado);
        return salario;
    }
}
