package com.claudio.financeiro.config;

import com.claudio.financeiro.service.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiterService rateLimiterService;

    public RateLimitInterceptor(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // Combina endpoint + IP para que login e recuperação de senha tenham cotas separadas
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();
        String identificador = uri + ":" + ip;

        if (rateLimiterService.excedeuLimite(identificador)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"erro\": \"Muitas tentativas. Aguarde 15 minutos antes de tentar novamente.\"}"
            );
            return false;
        }

        return true;
    }
}
