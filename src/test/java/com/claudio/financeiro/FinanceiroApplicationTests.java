package com.claudio.financeiro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

/** Smoke test: sobe o contexto do Spring (H2 em memória, sem SMTP real). */
@SpringBootTest
class FinanceiroApplicationTests {

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void contextLoads() {
    }
}
