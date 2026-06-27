package com.claudio.financeiro.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                /*
                 * ── HEADERS DE SEGURANÇA ──────────────────────────────────────
                 *
                 * X-Frame-Options: DENY
                 *   Impede que a aplicação seja carregada dentro de um <iframe>.
                 *   Protege contra ataques de Clickjacking.
                 *
                 * X-Content-Type-Options: nosniff
                 *   Impede que o browser tente "adivinhar" o tipo de conteúdo
                 *   de uma resposta. Protege contra MIME-sniffing attacks.
                 *
                 * HSTS (HTTP Strict Transport Security):
                 *   Instrui o browser a usar HTTPS por 1 ano, incluindo subdomínios.
                 *   Após a primeira visita, requisições HTTP são automaticamente
                 *   convertidas para HTTPS pelo browser.
                 *
                 * Referrer-Policy: STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                 *   Controla o que é enviado no cabeçalho Referer.
                 *   Em requisições cross-origin, envia apenas a origem (não o path).
                 *   Evita vazar URLs internas para serviços de terceiros.
                 *
                 * Content-Security-Policy (CSP):
                 *   Define de onde scripts, estilos e outros recursos podem ser
                 *   carregados. Reduz significativamente o impacto de ataques XSS.
                 *
                 *   'unsafe-inline' e 'unsafe-eval' são necessários para o Swagger UI
                 *   (que usa inline scripts e eval). Em produção, o Swagger é desabilitado
                 *   via application-prod.properties — uma CSP mais restrita pode ser
                 *   aplicada removendo essas diretivas nesse ambiente.
                 */
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                        )
                        .addHeaderWriter(new StaticHeadersWriter(
                                "Content-Security-Policy",
                                "default-src 'self'; " +
                                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                                "style-src 'self' 'unsafe-inline'; " +
                                "img-src 'self' data:; " +
                                "font-src 'self'; " +
                                "connect-src 'self'; " +
                                "frame-ancestors 'none'"
                        ))
                )

                /*
                 * ── CORS ──────────────────────────────────────────────────────
                 *
                 * Cross-Origin Resource Sharing: define quais origens podem
                 * chamar esta API. Sem isso, o browser bloquearia chamadas do
                 * frontend hospedado em domínio diferente.
                 *
                 * allowCredentials não é necessário para JWT (que vai no header
                 * Authorization), mas está habilitado para compatibilidade futura
                 * com cookies de sessão.
                 */
                .cors(cors -> cors.configurationSource(request -> {
                    var config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of(
                            "https://meu-financeiro-pessoal.vercel.app",
                            "http://localhost:5173",  // Vite dev server (padrão)
                            "http://localhost:5174",  // Vite fallback quando 5173 está ocupada
                            "http://localhost:3000",
                            "http://localhost:3001"
                    ));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                }))

                // CSRF desabilitado: a API é stateless (JWT), não usa cookies de sessão.
                // CSRF protege contra requisições forjadas usando cookies — sem cookies,
                // não há o que proteger aqui.
                .csrf(csrf -> csrf.disable())

                // Stateless: o Spring não cria nem usa sessões HTTP.
                // Cada requisição deve conter o JWT no header Authorization.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                /*
                 * ── REGRAS DE AUTORIZAÇÃO ────────────────────────────────────
                 *
                 * A ordem importa: as regras mais específicas vêm primeiro.
                 * anyRequest().authenticated() no final age como "default deny".
                 */
                .authorizeHttpRequests(auth -> auth
                        // Preflight do CORS: o browser envia OPTIONS antes de qualquer
                        // requisição cross-origin — deve ser sempre permitido
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // Endpoints de autenticação: públicos por definição
                        .requestMatchers("/auth/**").permitAll()
                        // /error: usado internamente pelo Spring para erros de validação (@Valid)
                        // Sem este permitAll, erros de validação retornam 403 em vez de 400
                        .requestMatchers("/error").permitAll()
                        // Swagger UI: permitido em dev (desabilitado em prod via properties)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Todo o resto exige autenticação via JWT
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
