package com.claudio.financeiro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de limitação de taxa de requisições (rate limiting).
 *
 * Problema que resolve:
 * Sem rate limiting, um atacante pode tentar senhas em loop ou inundar
 * a caixa de e-mail de qualquer usuário com códigos de recuperação.
 *
 * Como funciona — janela deslizante (sliding window):
 * Para cada identificador (ex: "login:127.0.0.1"), mantemos uma lista dos
 * timestamps das últimas tentativas. A cada nova requisição, removemos as
 * tentativas que ficaram fora da janela de tempo e checamos se o número
 * restante ultrapassou o limite. Se sim, bloqueamos.
 *
 * Limitações desta implementação (educacional):
 * - Dados em memória: reiniciar a aplicação zera o histórico.
 * - Não distribui entre múltiplas instâncias (não funciona em cluster).
 * - A lista de IPs cresce indefinidamente (sem limpeza periódica).
 *
 * Para produção real: use Bucket4j + Redis para rate limiting persistente
 * e distribuído.
 *
 * Thread-safety: ConcurrentHashMap + compute() garantem operações atômicas
 * sem race conditions entre requisições paralelas.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    /** Janela de tempo de 15 minutos (em milissegundos). */
    private static final long JANELA_MS = 15 * 60 * 1000L;

    /** Máximo de tentativas permitidas dentro da janela. */
    private static final int LIMITE_MAXIMO = 10;

    /**
     * Mapa: identificador → lista de timestamps das tentativas recentes.
     * ConcurrentHashMap porque múltiplas threads (requisições HTTP) podem
     * acessar e modificar o mapa simultaneamente.
     */
    private final ConcurrentHashMap<String, List<Long>> tentativas = new ConcurrentHashMap<>();

    /**
     * Verifica se o identificador excedeu o limite de tentativas.
     *
     * @param identificador chave única (ex: "login:192.168.1.1")
     * @return true se o limite foi excedido (requisição deve ser bloqueada)
     */
    public boolean excedeuLimite(String identificador) {
        long agora = System.currentTimeMillis();

        /*
         * compute() é atômico: lê e escreve o valor para a chave em uma
         * operação indivisível, sem necessidade de synchronized.
         */
        tentativas.compute(identificador, (chave, lista) -> {
            if (lista == null) {
                lista = new ArrayList<>();
            }
            // Remove tentativas que saíram da janela de tempo
            lista.removeIf(timestamp -> agora - timestamp > JANELA_MS);
            // Registra a tentativa atual
            lista.add(agora);
            return lista;
        });

        int total = tentativas.get(identificador).size();

        if (total > LIMITE_MAXIMO) {
            log.warn("Rate limit excedido para '{}': {} tentativas em {} minutos",
                    identificador, total, JANELA_MS / 60000);
            return true;
        }

        return false;
    }
}
