package com.claudio.financeiro.controller;

import com.claudio.financeiro.dto.AtualizarPerfilRequest;
import com.claudio.financeiro.dto.UsuarioDTO;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuario")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public UsuarioDTO me(Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return usuarioService.buscarPerfil(usuarioLogado.getId());
    }

    @PutMapping("/perfil")
    public UsuarioDTO atualizarPerfil(@Valid @RequestBody AtualizarPerfilRequest request, Authentication authentication) {
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();
        return usuarioService.atualizarPerfil(usuarioLogado.getId(), request);
    }
}
