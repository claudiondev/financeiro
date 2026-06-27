package com.claudio.financeiro;

import com.claudio.financeiro.controller.AuthController;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.UsuarioRepository;
import com.claudio.financeiro.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do AuthController.
 *
 * Usa mocks para todas as dependências (repositório, encoder, JWT, e-mail)
 * para que cada teste valide apenas a lógica do controller isoladamente.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthController authController;

    // -------------------------------------------------------------------------
    // registrar()
    // -------------------------------------------------------------------------

    /**
     * Cadastro com e-mail novo deve salvar o usuário e retornar 200.
     */
    @Test
    void deveRegistrarUsuarioComEmailNovo() {
        Usuario usuario = usuario("novo@teste.com", "123456");
        when(usuarioRepository.findByEmail("novo@teste.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any())).thenReturn(usuario);

        ResponseEntity<String> resposta = authController.registrar(usuario);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("Usuario registrado com sucesso!", resposta.getBody());
        verify(usuarioRepository).save(any());
    }

    /**
     * Cadastro com e-mail já existente deve retornar 409 Conflict
     * e NÃO salvar nada no banco.
     *
     * Este teste cobre o bug de e-mail duplicado descoberto durante os
     * testes manuais de endpoint.
     */
    @Test
    void deveRejeitarRegistroComEmailJaCadastrado() {
        Usuario existente = usuario("existente@teste.com", "senhaHash");
        when(usuarioRepository.findByEmail("existente@teste.com"))
                .thenReturn(Optional.of(existente));

        ResponseEntity<String> resposta = authController.registrar(existente);

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals("E-mail já cadastrado.", resposta.getBody());
        verify(usuarioRepository, never()).save(any()); // banco não é tocado
    }

    // -------------------------------------------------------------------------
    // login()
    // -------------------------------------------------------------------------

    /**
     * Login com credenciais corretas deve retornar o token JWT.
     */
    @Test
    void deveRetornarTokenNoLoginComCredenciaisCorretas() {
        Usuario usuario = usuario("claudio@teste.com", "hashDaSenha");
        when(usuarioRepository.findByEmail("claudio@teste.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hashDaSenha")).thenReturn(true);
        when(jwtService.gerarToken("claudio@teste.com")).thenReturn("jwt.token.aqui");

        Usuario loginRequest = usuario("claudio@teste.com", "123456");
        ResponseEntity<String> resposta = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("jwt.token.aqui", resposta.getBody());
    }

    /**
     * Login com e-mail não cadastrado deve retornar 401 com mensagem genérica.
     * Não deve revelar que o e-mail não existe (proteção contra enumeração).
     */
    @Test
    void deveRetornarMensagemGenericaParaEmailNaoCadastrado() {
        when(usuarioRepository.findByEmail("inexistente@teste.com"))
                .thenReturn(Optional.empty());

        ResponseEntity<String> resposta = authController.login(usuario("inexistente@teste.com", "qualquer"));

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertEquals("Credenciais inválidas", resposta.getBody());
    }

    /**
     * Login com senha errada deve retornar 401 com a mesma mensagem genérica.
     * A mensagem é idêntica à do e-mail errado — não revela qual dos dois falhou.
     */
    @Test
    void deveRetornarMensagemGenericaParaSenhaErrada() {
        Usuario usuario = usuario("claudio@teste.com", "hashCorreto");
        when(usuarioRepository.findByEmail("claudio@teste.com"))
                .thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "hashCorreto")).thenReturn(false);

        ResponseEntity<String> resposta = authController.login(usuario("claudio@teste.com", "senhaErrada"));

        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
        assertEquals("Credenciais inválidas", resposta.getBody());
    }

    // -------------------------------------------------------------------------
    // Método auxiliar
    // -------------------------------------------------------------------------

    private Usuario usuario(String email, String senha) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setSenha(senha);
        return u;
    }
}
