package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.CriarSalarioRequest;
import com.claudio.financeiro.dto.SalarioDTO;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SalarioService {

    private final SalarioRepository salarioRepository;

    public SalarioService(SalarioRepository salarioRepository) {
        this.salarioRepository = salarioRepository;
    }

    public SalarioDTO criar(CriarSalarioRequest request, Usuario usuarioLogado) {
        Salario salario = paraEntidade(request, usuarioLogado);
        return toDTO(salvar(salario));
    }

    public SalarioDTO atualizar(Long id, CriarSalarioRequest request, Long usuarioId) {
        Salario salario = buscarComOwnership(id, usuarioId);
        salario.setValor(request.getValor());
        salario.setComissao(request.getComissao());
        salario.setAdicional(request.getAdicional());
        salario.setDescricao(request.getDescricao());
        salario.setData(request.getData());
        return toDTO(salvar(salario));
    }

    public List<SalarioDTO> listarPorUsuario(Long usuarioId) {
        return salarioRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    // Mês/ano nulos = sem filtro (retorna tudo), mesmo padrão de GastoRepository.findByFiltros
    public List<SalarioDTO> filtrarPorPeriodo(Long usuarioId, Integer mes, Integer ano) {
        return salarioRepository.findByUsuarioId(usuarioId).stream()
                .filter(s -> CalculoFinanceiroUtil.salariosNoPeriodo(s, mes, ano))
                .map(this::toDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    // Verifica ownership antes de deletar, para evitar IDOR (usuário apagando salário de outro)
    public void deletar(Long id, Long usuarioId) {
        buscarComOwnership(id, usuarioId);
        salarioRepository.deleteById(id);
    }

    private Salario salvar(Salario salario) {
        return salarioRepository.save(salario);
    }

    private SalarioDTO toDTO(Salario salario) {
        return new SalarioDTO(
                salario.getId(),
                salario.getValor(),
                salario.getComissao(),
                salario.getAdicional(),
                salario.getDescricao(),
                salario.getData(),
                salario.getUsuario() != null ? salario.getUsuario().getId() : null
        );
    }

    private Salario paraEntidade(CriarSalarioRequest request, Usuario usuarioLogado) {
        Salario salario = new Salario();
        salario.setValor(request.getValor());
        salario.setComissao(request.getComissao());
        salario.setAdicional(request.getAdicional());
        salario.setDescricao(request.getDescricao());
        salario.setData(request.getData());
        salario.setUsuario(usuarioLogado);
        return salario;
    }

    private Salario buscarComOwnership(Long id, Long usuarioId) {
        Salario salario = salarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salário não encontrado"));

        if (!salario.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return salario;
    }
}
