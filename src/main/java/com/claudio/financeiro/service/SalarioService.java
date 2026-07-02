package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.SalarioDTO;
import com.claudio.financeiro.model.Salario;
import com.claudio.financeiro.repository.SalarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SalarioService {

    @Autowired
    private SalarioRepository salarioRepository;

    public Salario salvar(Salario salario) {
        return salarioRepository.save(salario);
    }

    public List<Salario> listarPorUsuario(Long usuarioId) {
        return salarioRepository.findByUsuarioId(usuarioId);
    }

    // Verifica ownership antes de deletar, para evitar IDOR (usuário apagando salário de outro)
    public void deletar(Long id, Long usuarioId) {
        Salario salario = salarioRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Salário não encontrado"));

        if (!salario.getUsuario().getId().equals(usuarioId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Acesso negado");
        }

        salarioRepository.deleteById(id);
    }

    public SalarioDTO toDTO(Salario salario) {
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
}
