package com.claudio.financeiro;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Teste de fumaça (smoke test): verifica que o contexto do Spring sobe sem erros.
 *
 * Por que @MockBean no JavaMailSender?
 * O AuthController depende de JavaMailSender para enviar e-mails de recuperação
 * de senha. Nos testes, não queremos conectar em nenhum servidor SMTP real —
 * o MockBean substitui o bean por um objeto falso que não faz nada.
 *
 * O banco de dados é substituído pelo H2 em memória via
 * src/test/resources/application.properties.
 */
@SpringBootTest
class FinanceiroApplicationTests {

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void contextLoads() {
        // Se o contexto do Spring subir sem exceção, o teste passa.
        // Isso valida que todas as dependências estão configuradas corretamente,
        // que o JwtService inicializa sem erros, e que o banco (H2) está acessível.
    }
}
