package com.claudio.financeiro;

import com.claudio.financeiro.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET_VALIDO = "segredo-de-teste-com-minimo-de-32-caracteres-aqui";
    private static final long EXPIRACAO_1_HORA = 3_600_000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET_VALIDO);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRACAO_1_HORA);
        jwtService.init();
    }

    @Test
    void deveGerarTokenNaoNulo() {
        String token = jwtService.gerarToken("usuario@teste.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void deveGerarTokenNoFormatoJwt() {
        String token = jwtService.gerarToken("usuario@teste.com");
        String[] partes = token.split("\\.");

        assertEquals(3, partes.length, "JWT deve ter 3 partes: header.payload.signature");
    }

    @Test
    void deveExtrairEmailDoToken() {
        String email = "claudio@financeiro.com";
        String token = jwtService.gerarToken(email);

        assertEquals(email, jwtService.extrairEmail(token));
    }

    @Test
    void deveValidarTokenComEmailCorreto() {
        String email = "usuario@teste.com";
        String token = jwtService.gerarToken(email);

        assertTrue(jwtService.validarToken(token, email));
    }

    @Test
    void deveRejeitarTokenComEmailDiferente() {
        String token = jwtService.gerarToken("a@teste.com");

        assertFalse(jwtService.validarToken(token, "b@teste.com"));
    }

    @Test
    void deveRejeitarTokenComAssinaturaAdulterada() {
        String token = jwtService.gerarToken("usuario@teste.com") + "adulterado";

        assertFalse(jwtService.validarToken(token, "usuario@teste.com"));
    }

    // Expiração de 1ms força o token a expirar antes da validação
    @Test
    void deveRejeitarTokenExpirado() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "expiration", 1L);
        jwtService.init();

        String token = jwtService.gerarToken("usuario@teste.com");
        Thread.sleep(20);

        assertFalse(jwtService.validarToken(token, "usuario@teste.com"));
    }

    @Test
    void tokenRevogadoDeveRetornarFalseQuandoSenhaNuncaFoiTrocada() {
        String token = jwtService.gerarToken("usuario@teste.com");

        assertFalse(jwtService.tokenRevogado(token, null));
    }

    @Test
    void tokenRevogadoDeveRetornarTrueQuandoEmitidoAntesDaTrocaDeSenha() {
        String token = jwtService.gerarToken("usuario@teste.com");
        LocalDateTime senhaTrocadaDepois = LocalDateTime.now().plusMinutes(1);

        assertTrue(jwtService.tokenRevogado(token, senhaTrocadaDepois));
    }

    @Test
    void tokenRevogadoDeveRetornarFalseQuandoEmitidoDepoisDaTrocaDeSenha() {
        LocalDateTime senhaTrocadaAntes = LocalDateTime.now().minusMinutes(1);
        String token = jwtService.gerarToken("usuario@teste.com");

        assertFalse(jwtService.tokenRevogado(token, senhaTrocadaAntes));
    }

    @Test
    void deveRejeitarSegredoMenorQue32Caracteres() {
        JwtService serviceInvalido = new JwtService();
        ReflectionTestUtils.setField(serviceInvalido, "secret", "curto");
        ReflectionTestUtils.setField(serviceInvalido, "expiration", EXPIRACAO_1_HORA);

        assertThrows(IllegalStateException.class, serviceInvalido::init);
    }
}
