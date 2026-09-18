package com.claudio.financeiro;

import com.claudio.financeiro.service.QuotaAssistenteService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class QuotaAssistenteServiceTest {

    @Test
    void quotaDemoResisteAChamadasConcorrentes() throws Exception {
        String banco = "jdbc:h2:mem:quota_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(banco, "sa", ""));
        jdbc.execute("CREATE TABLE quota_assistente (data_quota DATE NOT NULL, escopo VARCHAR(80) NOT NULL, " +
                "utilizado INT NOT NULL DEFAULT 0, PRIMARY KEY (data_quota, escopo))");
        QuotaAssistenteService quota = new QuotaAssistenteService(jdbc);

        var executor = Executors.newFixedThreadPool(8);
        try {
            var chamadas = IntStream.range(0, 35)
                    .mapToObj(i -> executor.submit(() -> {
                        try { quota.reservar(1L, true); return true; }
                        catch (ResponseStatusException ex) { return false; }
                    })).toList();
            int aceitas = 0;
            for (Future<Boolean> chamada : chamadas) if (chamada.get()) aceitas++;
            assertEquals(30, aceitas);
        } finally {
            executor.shutdownNow();
        }
        assertEquals(30, jdbc.queryForObject(
                "SELECT utilizado FROM quota_assistente WHERE escopo = 'demo'", Integer.class));
    }
}
