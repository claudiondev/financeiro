package com.claudio.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/registrar")
    public String registrar(@RequestBody Usuario usuario) {usuario.setSenha(passwordEncoder.encode(usuario.getSenha())); usuarioRepository.save(usuario);
        return "Usuário registrado com sucesso!";
    }

    @PostMapping("/login")
    public String login  (@RequestBody Usuario usuario) {
    Usuario encontrado =
            usuarioRepository.findByEmail(usuario.getEmail()).orElseThrow();
        if (!passwordEncoder.matches(usuario.getSenha(), encontrado.getSenha()))
         return "Senha incorreta";
         return  jwtService.gerarToken(encontrado.getEmail());

        }


    }


