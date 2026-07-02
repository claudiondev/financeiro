package com.claudio.financeiro.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting por janela deslizante: para cada identificador (ex: "login:127.0.0.1"),
 * guarda os timestamps das tentativas recentes e bloqueia quando excedem o limite.
 *
 * Implementação em memória — não distribui entre múltiplas instâncias. Para produção
 * com múltiplos nós, considerar Bucket4j + Redis.
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private static final long JANELA_MS = 15 * 60 * 1000L;
    private static final int LIMITE_MAXIMO = 10;

    private final ConcurrentHashMap<String, List<Long>> tentativas = new ConcurrentHashMap<>();

    public boolean excedeuLimite(String identificador) {
        long agora = System.currentTimeMillis();

        // compute() é atômico, evitando race condition entre requisições paralelas
        tentativas.compute(identificador, (chave, lista) -> {
            if (lista == null) {
                lista = new ArrayList<>();
            }
            lista.removeIf(timestamp -> agora - timestamp > JANELA_MS);
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
