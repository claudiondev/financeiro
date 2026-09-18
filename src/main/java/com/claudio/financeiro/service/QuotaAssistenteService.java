package com.claudio.financeiro.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class QuotaAssistenteService {

    private static final ZoneId FUSO = ZoneId.of("America/Recife");
    private final JdbcTemplate jdbc;

    public QuotaAssistenteService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void reservar(Long usuarioId, boolean demo) {
        LocalDate hoje = LocalDate.now(FUSO);
        if (demo) {
            reservarEscopo(hoje, "demo", 30);
        } else {
            reservarEscopo(hoje, "usuario:" + usuarioId, 20);
        }
        // A reserva global é feita por último. Um bloqueio global pode consumir a quota
        // individual, mas nunca libera chamadas extras para a OpenAI.
        reservarEscopo(hoje, "global", 50);
    }

    private void reservarEscopo(LocalDate hoje, String escopo, int limite) {
        try {
            jdbc.update("INSERT INTO quota_assistente (data_quota, escopo, utilizado) VALUES (?, ?, 0)", hoje, escopo);
        } catch (DuplicateKeyException ignored) {
            // Outra requisição já criou a linha deste dia. A atualização abaixo é atômica.
        }
        int alteradas = jdbc.update("UPDATE quota_assistente SET utilizado = utilizado + 1 " +
                "WHERE data_quota = ? AND escopo = ? AND utilizado < ?", hoje, escopo, limite);
        if (alteradas == 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    escopo.equals("demo") ? "Limite diário da demo atingido" : "Limite diário do assistente atingido");
        }
    }
}
