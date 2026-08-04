package com.claudio.financeiro;

import com.claudio.financeiro.config.DemoReadOnlyInterceptor;
import com.claudio.financeiro.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoReadOnlyInterceptorTest {

    private final DemoReadOnlyInterceptor interceptor = new DemoReadOnlyInterceptor();

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void deveBloquearEscritaDoUsuarioDemo(String metodo) throws Exception {
        autenticarComo(usuario(true));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn(metodo);
        StringWriter corpo = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(corpo));

        boolean continua = interceptor.preHandle(request, response, new Object());

        assertFalse(continua);
        verify(response).setStatus(423);
        assertTrue(corpo.toString().contains("modo demo"));
    }

    @Test
    void devePermitirEscritaDeUsuarioNormal() throws Exception {
        autenticarComo(usuario(false));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");

        boolean continua = interceptor.preHandle(request, response, new Object());

        assertTrue(continua);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void devePermitirLeituraMesmoDoUsuarioDemo() throws Exception {
        autenticarComo(usuario(true));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("GET");

        boolean continua = interceptor.preHandle(request, response, new Object());

        assertTrue(continua);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void devePermitirEscritaSemAutenticacao() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");

        boolean continua = interceptor.preHandle(request, response, new Object());

        assertTrue(continua);
    }

    private Usuario usuario(boolean demo) {
        Usuario usuario = new Usuario();
        usuario.setEmail(demo ? "demo@meufinanceiro.app" : "claudio@teste.com");
        usuario.setDemo(demo);
        return usuario;
    }

    private void autenticarComo(Usuario usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities())
        );
    }
}
