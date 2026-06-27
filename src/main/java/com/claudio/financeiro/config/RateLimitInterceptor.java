package com.claudio.financeiro.config;

import com.claudio.financeiro.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor HTTP que aplica rate limiting nos endpoints de autenticação.
 *
 * Por que usar um Interceptor em vez de colocar a lógica no Controller?
 *
 * Rate limiting é uma preocupação transversal (cross-cutting concern): ela
 * não faz parte da regra de negócio do login ou da recuperação de senha —
 * ela protege a infraestrutura. O princípio de Responsabilidade Única (SRP)
 * diz que o AuthController não deveria misturar regras de negócio com
 * controle de tráfego.
 *
 * O HandlerInterceptor intercepta a requisição ANTES de chegar ao controller,
 * o que é exatamente o ponto certo para bloquear o acesso sem nem executar
 * a lógica de negócio.
 *
 * Alternativa: filtro do Spring Security. A diferença é que o interceptor
 * roda DEPOIS do Spring Security (autenticação já resolvida), enquanto um
 * filtro roda antes. Para rate limiting por IP em endpoints públicos, ambos
 * funcionam. O interceptor é mais simples e integrado ao MVC.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        /*
         * O identificador combina o endpoint com o IP do cliente.
         * Isso limita por endpoint individualmente:
         * "login:192.168.1.1" e "recuperar-senha:192.168.1.1" têm contadores
         * separados, evitando que tentativas de recuperação de senha consumam
         * a cota do login.
         */
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String identificador = uri + ":" + ip;

        if (rateLimiterService.excedeuLimite(identificador)) {
            response.setStatus(429); // 429 Too Many Requests (RFC 6585)
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"erro\": \"Muitas tentativas. Aguarde 15 minutos antes de tentar novamente.\"}"
            );
            return false; // interrompe a cadeia — o controller NÃO é chamado
        }

        return true; // prossegue normalmente
    }
}
