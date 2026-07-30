package com.claudio.financeiro;

import com.claudio.financeiro.dto.AtualizarPerfilRequest;
import com.claudio.financeiro.dto.UsuarioDTO;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.UsuarioRepository;
import com.claudio.financeiro.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void deveRetornarPerfilComNomeEEmail() {
        Usuario usuario = usuarioComId(1L, "claudio@teste.com", "Claudio");
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioDTO resultado = usuarioService.buscarPerfil(1L);

        assertEquals("Claudio", resultado.getNome());
        assertEquals("claudio@teste.com", resultado.getEmail());
    }

    @Test
    void deveRetornarPerfilComNomeNuloQuandoContaAntigaNaoTemNome() {
        Usuario usuario = usuarioComId(1L, "claudio@teste.com", null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioDTO resultado = usuarioService.buscarPerfil(1L);

        assertNull(resultado.getNome());
    }

    @Test
    void deveLancarNotFoundAoBuscarPerfilDeUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.buscarPerfil(99L)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void deveAtualizarNomeDoPerfil() {
        Usuario usuario = usuarioComId(1L, "claudio@teste.com", null);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDTO resultado = usuarioService.atualizarPerfil(1L, new AtualizarPerfilRequest("Claudio Nascimento"));

        assertEquals("Claudio Nascimento", resultado.getNome());
        verify(usuarioRepository).save(usuario);
    }

    private Usuario usuarioComId(Long id, String email, String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setNome(nome);
        return usuario;
    }
}
