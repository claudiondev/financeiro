package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.AtualizarPerfilRequest;
import com.claudio.financeiro.dto.UsuarioDTO;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario não encontrado"));
    }

    public UsuarioDTO buscarPerfil(Long usuarioId) {
        return toDTO(buscarPorId(usuarioId));
    }

    public UsuarioDTO atualizarPerfil(Long usuarioId, AtualizarPerfilRequest request) {
        Usuario usuario = buscarPorId(usuarioId);
        usuario.setNome(request.getNome());
        return toDTO(usuarioRepository.save(usuario));
    }

    private Usuario buscarPorId(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private UsuarioDTO toDTO(Usuario usuario) {
        return new UsuarioDTO(usuario.getNome(), usuario.getEmail());
    }
}