package com.claudio.financeiro.config;

import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.service.JwtService;
import com.claudio.financeiro.service.UsuarioService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public JwtFilter(JwtService jwtService, UsuarioService usuarioService) {
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }
            String token = authHeader.substring(7);
            String email = jwtService.extrairEmail(token);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = usuarioService.loadUserByUsername(email);
                if (userDetails instanceof Usuario usuario && jwtService.tokenRevogado(token, usuario.getSenhaAlteradaEm())) {
                    log.debug("Token revogado (emitido antes da última troca de senha) para {}", email);
                    filterChain.doFilter(request, response);
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (ExpiredJwtException e) {
            log.debug("Token expirado na requisição para {}", request.getRequestURI());
        } catch (JwtException e) {
            log.warn("Token JWT inválido em {}: {}", request.getRequestURI(), e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado no filtro JWT em {}: {}", request.getRequestURI(), e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}