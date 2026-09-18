package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.MensagemChatRequest;
import com.claudio.financeiro.dto.MensagemChatResposta;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.AssistenteChatService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/assistente/chat")
public class AssistenteChatController {

    private final AssistenteChatService chat;

    public AssistenteChatController(AssistenteChatService chat) { this.chat = chat; }

    @GetMapping("/status")
    public Map<String, Boolean> status() { return Map.of("habilitado", chat.habilitado()); }

    @PostMapping("/sessoes")
    public Map<String, String> criar(Authentication autenticacao) {
        return Map.of("sessao", chat.criarSessao(usuario(autenticacao).getId()));
    }

    @PostMapping("/mensagens")
    public MensagemChatResposta responder(@RequestHeader("X-Assistente-Sessao") String sessao,
                                          @Valid @RequestBody MensagemChatRequest request,
                                          Authentication autenticacao) {
        return chat.responder(sessao, usuario(autenticacao), request);
    }

    @DeleteMapping("/sessao")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void encerrar(@RequestHeader("X-Assistente-Sessao") String sessao,
                         Authentication autenticacao) {
        chat.encerrarSessao(sessao, usuario(autenticacao).getId());
    }

    private Usuario usuario(Authentication autenticacao) {
        return (Usuario) autenticacao.getPrincipal();
    }
}
