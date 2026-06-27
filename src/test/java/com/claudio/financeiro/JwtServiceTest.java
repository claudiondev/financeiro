package com.claudio.financeiro;

import com.claudio.financeiro.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do JwtService.
 *
 * JwtService não tem dependências de outros beans — só precisa de dois valores
 * (@Value): jwt.secret e jwt.expiration. Por isso não usamos @SpringBootTest
 * nem @ExtendWith(MockitoExtension.class): criamos o objeto diretamente com
 * "new" e injetamos os valores via ReflectionTestUtils.setField().
 *
 * ReflectionTestUtils é um utilitário do Spring Test que permite definir campos
 * privados em objetos sem precisar de setter — muito útil para testes de
 * classes que usam @Value.
 */
class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET_VALIDO = "segredo-de-teste-com-minimo-de-32-caracteres-aqui";
    private static final long EXPIRACAO_1_HORA = 3_600_000L;

    /**
     * @BeforeEach executa antes de cada teste.
     * Recria o JwtService do zero para garantir isolamento entre os testes.
     */
    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET_VALIDO);
        ReflectionTestUtils.setField(jwtService, "expiration", EXPIRACAO_1_HORA);
        jwtService.init(); // equivalente ao @PostConstruct — inicializa a chave HMAC
    }

    // -------------------------------------------------------------------------
    // gerarToken()
    // -------------------------------------------------------------------------

    /**
     * Um token gerado não deve ser nulo nem vazio.
     * Verifica que o método básico funciona sem exceções.
     */
    @Test
    void deveGerarTokenNaoNulo() {
        String token = jwtService.gerarToken("usuario@teste.com");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    /**
     * Um JWT é composto por 3 partes separadas por ponto: header.payload.signature
     * Verifica que o token gerado tem exatamente esse formato.
     */
    @Test
    void deveGerarTokenNoFormatoJwt() {
        String token = jwtService.gerarToken("usuario@teste.com");
        String[] partes = token.split("\\.");

        assertEquals(3, partes.length, "JWT deve ter 3 partes: header.payload.signature");
    }

    // -------------------------------------------------------------------------
    // extrairEmail()
    // -------------------------------------------------------------------------

    /**
     * O e-mail extraído do token deve ser idêntico ao que foi usado para gerá-lo.
     * Testa o ciclo completo: gerar → extrair.
     */
    @Test
    void deveExtrairEmailDoToken() {
        String email = "claudio@financeiro.com";
        String token = jwtService.gerarToken(email);

        assertEquals(email, jwtService.extrairEmail(token));
    }

    // -------------------------------------------------------------------------
    // validarToken()
    // -------------------------------------------------------------------------

    /**
     * Token válido com o e-mail correto deve retornar true.
     */
    @Test
    void deveValidarTokenComEmailCorreto() {
        String email = "usuario@teste.com";
        String token = jwtService.gerarToken(email);

        assertTrue(jwtService.validarToken(token, email));
    }

    /**
     * Token gerado para um e-mail NÃO deve ser válido para outro e-mail.
     * Garante que não é possível usar o token de um usuário para se autenticar
     * como outro usuário.
     */
    @Test
    void deveRejeitarTokenComEmailDiferente() {
        String token = jwtService.gerarToken("a@teste.com");

        assertFalse(jwtService.validarToken(token, "b@teste.com"));
    }

    /**
     * Token com assinatura adulterada (um caractere a mais no final) deve ser
     * rejeitado. Garante que a verificação criptográfica está funcionando.
     */
    @Test
    void deveRejeitarTokenComAssinaturaAdulterada() {
        String token = jwtService.gerarToken("usuario@teste.com") + "adulterado";

        assertFalse(jwtService.validarToken(token, "usuario@teste.com"));
    }

    /**
     * Token expirado deve retornar false.
     *
     * Configura uma expiração de 1ms, gera o token, espera 20ms e valida.
     * Thread.sleep() é aceitável aqui porque estamos testando comportamento
     * dependente de tempo — não existe outra forma de simular expiração real.
     */
    @Test
    void deveRejeitarTokenExpirado() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "expiration", 1L);
        jwtService.init();

        String token = jwtService.gerarToken("usuario@teste.com");
        Thread.sleep(20); // aguarda o token expirar

        assertFalse(jwtService.validarToken(token, "usuario@teste.com"));
    }

    // -------------------------------------------------------------------------
    // init() — validação do segredo
    // -------------------------------------------------------------------------

    /**
     * JwtService deve lançar IllegalStateException ao inicializar com segredo
     * menor que 32 caracteres (256 bits mínimos para HMAC-SHA256).
     *
     * Garante que a aplicação falha imediatamente no boot em vez de rodar
     * com segurança comprometida.
     */
    @Test
    void deveRejeitarSegredoMenorQue32Caracteres() {
        JwtService serviceInvalido = new JwtService();
        ReflectionTestUtils.setField(serviceInvalido, "secret", "curto");
        ReflectionTestUtils.setField(serviceInvalido, "expiration", EXPIRACAO_1_HORA);

        assertThrows(IllegalStateException.class, serviceInvalido::init);
    }
}
