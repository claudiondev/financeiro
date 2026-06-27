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

    // -------------------------------------------------------------------------
    // Operações básicas CRUD
    // -------------------------------------------------------------------------

    /**
     * Persiste o salário no banco e retorna a entidade salva (com ID gerado).
     * O controller chama toDTO() depois para obter a representação segura.
     */
    public Salario salvar(Salario salario) {
        return salarioRepository.save(salario);
    }

    /**
     * Lista todos os salários do usuário.
     * Retorna entidades — o controller converte para DTO antes de enviar ao cliente.
     *
     * Por que não retornar DTOs diretamente?
     * Os testes unitários verificam o comportamento do serviço com entidades
     * reais. Mudar a assinatura quebraria esses testes sem necessidade.
     * A conversão é responsabilidade do controller, que sabe o que o cliente
     * precisa receber.
     */
    public List<Salario> listarPorUsuario(Long usuarioId) {
        return salarioRepository.findByUsuarioId(usuarioId);
    }

    /**
     * Remove um salário, mas apenas se pertencer ao usuário autenticado.
     * Proteção contra IDOR: usuário A não pode deletar salário do usuário B.
     */
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

    // -------------------------------------------------------------------------
    // Conversão Entidade → DTO (público para uso no controller)
    // -------------------------------------------------------------------------

    /**
     * Converte a entidade Salario para SalarioDTO.
     *
     * A conversão está no serviço (não no controller) para centralizar o
     * mapeamento. Se o DTO mudar, só este método precisa ser atualizado.
     *
     * O campo usuarioId substitui o objeto Usuario completo, evitando
     * expor email e outros campos internos na resposta JSON.
     */
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
