package com.claudio.financeiro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AssistenteChatEndpointTest {

    @Autowired MockMvc mvc;
    @MockBean JavaMailSender mailSender;

    @Test
    void demoMantemInsightsERecebe503QuandoChatSemChave() throws Exception {
        String token = mvc.perform(post("/auth/demo"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        mvc.perform(get("/assistente/insights").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/assistente/chat/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.habilitado").value(false));
        mvc.perform(post("/assistente/chat/sessoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable());
    }
}
