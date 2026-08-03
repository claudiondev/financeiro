package com.claudio.financeiro.service;

import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final SecureRandom secureRandom = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public AuthService(UsuarioRepository usuarioRepository, JwtService jwtService,
                       PasswordEncoder passwordEncoder, JavaMailSender mailSender) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public void registrar(String email, String senha, String nome) {
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setNome(nome);
        usuarioRepository.save(usuario);
    }

    public String login(String email, String senha) {
        Usuario encontrado = usuarioRepository.findByEmail(email).orElse(null);

        if (encontrado == null || !passwordEncoder.matches(senha, encontrado.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }

        return jwtService.gerarToken(encontrado.getEmail());
    }

    public void recuperarSenha(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            String codigo = String.valueOf(secureRandom.nextInt(900000) + 100000);
            usuario.setCodigoRecuperacao(codigo);
            usuario.setCodigoRecuperacaoExpiracao(LocalDateTime.now().plusMinutes(15));
            usuarioRepository.save(usuario);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Recuperação de senha");
            message.setText("Seu código de recuperação é: " + codigo + "\nEle expira em 15 minutos.");
            mailSender.send(message);
        });
    }

    public void redefinirSenha(String codigo, String novaSenha) {
        Usuario usuario = usuarioRepository.findByCodigoRecuperacao(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código inválido."));

        if (usuario.getCodigoRecuperacaoExpiracao() == null
                || LocalDateTime.now().isAfter(usuario.getCodigoRecuperacaoExpiracao())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código expirado. Solicite um novo.");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setSenhaAlteradaEm(LocalDateTime.now());
        usuario.setCodigoRecuperacao(null);
        usuario.setCodigoRecuperacaoExpiracao(null);
        usuarioRepository.save(usuario);
    }
}
