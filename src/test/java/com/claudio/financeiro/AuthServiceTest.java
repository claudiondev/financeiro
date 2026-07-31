package com.claudio.financeiro;

import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.UsuarioRepository;
import com.claudio.financeiro.service.AuthService;
import com.claudio.financeiro.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthService authService;

    @Test
    void deveRegistrarUsuarioComEmailNovo() {
        when(usuarioRepository.findByEmail("novo@teste.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("123456")).thenReturn("$2a$hash");

        authService.registrar("novo@teste.com", "123456", "Claudio");

        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void deveRejeitarRegistroComEmailJaCadastrado() {
        when(usuarioRepository.findByEmail("existente@teste.com"))
                .thenReturn(Optional.of(new Usuario()));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.registrar("existente@teste.com", "123456", null)
        );

        assertEquals(409, ex.getStatusCode().value());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveRetornarTokenNoLoginComCredenciaisCorretas() {
        Usuario usuario = usuario("claudio@teste.com", "hashDaSenha");
        when(usuarioRepository.findByEmail("claudio@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("123456", "hashDaSenha")).thenReturn(true);
        when(jwtService.gerarToken("claudio@teste.com")).thenReturn("jwt.token.aqui");

        String token = authService.login("claudio@teste.com", "123456");

        assertEquals("jwt.token.aqui", token);
    }

    @Test
    void deveLancar401ParaEmailNaoCadastrado() {
        when(usuarioRepository.findByEmail("inexistente@teste.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login("inexistente@teste.com", "qualquer")
        );

        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void deveLancar401ParaSenhaErrada() {
        Usuario usuario = usuario("claudio@teste.com", "hashCorreto");
        when(usuarioRepository.findByEmail("claudio@teste.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "hashCorreto")).thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> authService.login("claudio@teste.com", "senhaErrada")
        );

        assertEquals(401, ex.getStatusCode().value());
    }

    private Usuario usuario(String email, String senha) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setSenha(senha);
        return u;
    }
}
