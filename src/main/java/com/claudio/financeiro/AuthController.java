package com.claudio.financeiro;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;



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

    @PostMapping("/recuperar-senha")
    public String recuperarSenha(@RequestBody String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();

        String codigo = String.valueOf((int)(Math.random() * 900000) + 100000);
        usuario.setCodigoRecuperacao(codigo);
        usuarioRepository.save(usuario);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Recuperação de senha");
        message.setText("Seu código de recuperação é: " + codigo);
        mailSender.send(message);

        return "Código enviado para o email!";
    }

    @PostMapping("/redefinir-senha")
    public String redefinirSenha (@RequestBody RedefinirSenhaRequest request) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario usuario : usuarios) {
            if (request.getCodigo().equals(usuario.getCodigoRecuperacao())) {
                usuario.setSenha(passwordEncoder.encode(request.getNovaSenha()));
                usuarioRepository.save(usuario);

                return "Senha redefinida com sucesso!";
            }
        }

        return "Código invalido!";

    }

}


