package com.claudio.financeiro;

import com.claudio.financeiro.service.RateLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IntegracaoEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private RateLimiterService rateLimiterService;

    private String tokenUsuarioA;
    private String tokenUsuarioB;

    @BeforeEach
    void setUp() throws Exception {
        when(rateLimiterService.excedeuLimite(anyString())).thenReturn(false);
        tokenUsuarioA = registrarELogar("usera_" + System.nanoTime() + "@teste.com", "Senha123");
        tokenUsuarioB = registrarELogar("userb_" + System.nanoTime() + "@teste.com", "Senha456");
    }

    @Test
    void deveRejeitar403SemToken() throws Exception {
        mockMvc.perform(get("/gastos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRejeitar403ComTokenInvalido() throws Exception {
        mockMvc.perform(get("/gastos")
                        .header("Authorization", "Bearer token-invalido-qualquer"))
                .andExpect(status().isForbidden());
    }

    @Test
    void devePermitirRegistroELoginSemAutenticacao() throws Exception {
        String email = "novo_" + System.nanoTime() + "@teste.com";

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "senha", "MinhaSenha1"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "senha", "MinhaSenha1"))))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString())));
    }

    @Test
    void deveRetornar400AoRegistrarComSenhaFraca() throws Exception {
        String email = "senhafraca_" + System.nanoTime() + "@teste.com";

        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", email, "senha", "semmaiuscula1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deveCriarEListarGastoComTokenValido() throws Exception {
        Map<String, Object> gasto = Map.of(
                "descricao", "Teste integração",
                "valor", 99.90,
                "categoria", "ALIMENTACAO",
                "data", "2026-07-31"
        );

        mockMvc.perform(post("/gastos")
                        .header("Authorization", "Bearer " + tokenUsuarioA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gasto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gasto.descricao").value("Teste integração"))
                .andExpect(jsonPath("$.gasto.valor").value(99.90));

        mockMvc.perform(get("/gastos")
                        .header("Authorization", "Bearer " + tokenUsuarioA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.descricao == 'Teste integração')]").exists());
    }

    @Test
    void deveRetornar400ParaInputInvalido() throws Exception {
        Map<String, Object> gastoInvalido = Map.of(
                "descricao", "",
                "valor", -10,
                "data", "2026-07-31"
        );

        mockMvc.perform(post("/gastos")
                        .header("Authorization", "Bearer " + tokenUsuarioA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gastoInvalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void deveImpedirAcessoAGastoDeOutroUsuario() throws Exception {
        Map<String, Object> gasto = Map.of(
                "descricao", "Gasto do A",
                "valor", 50.0,
                "categoria", "OUTROS",
                "data", "2026-07-31"
        );

        MvcResult criacao = mockMvc.perform(post("/gastos")
                        .header("Authorization", "Bearer " + tokenUsuarioA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gasto)))
                .andExpect(status().isOk())
                .andReturn();

        Long gastoId = objectMapper.readTree(criacao.getResponse().getContentAsString()).get("gasto").get("id").asLong();

        mockMvc.perform(delete("/gastos/" + gastoId)
                        .header("Authorization", "Bearer " + tokenUsuarioB))
                .andExpect(status().isForbidden());
    }

    private String registrarELogar(String email, String senha) throws Exception {
        mockMvc.perform(post("/auth/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", senha))))
                .andExpect(status().isOk());

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", senha))))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString().trim();
        if (body.startsWith("\"") && body.endsWith("\"")) {
            body = body.substring(1, body.length() - 1);
        }
        return body;
    }
}
