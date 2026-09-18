package com.claudio.financeiro;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "app.assistente.habilitado=true",
        "app.assistente.chave=chave-ficticia-para-teste"
})
class AssistenteIaConfigTest {

    @Autowired ChatClient chatClient;
    @MockBean JavaMailSender mailSender;

    @Test
    void criaClienteQuandoHabilitadoSemAcessarRede() {
        assertNotNull(chatClient);
    }
}
