package com.claudio.financeiro.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AssistenteIaConfig {

    // A auto-configuração do starter fica desligada: sem chave, nenhum bean do provedor é criado.
    @Bean
    @ConditionalOnExpression("'${app.assistente.habilitado:false}' == 'true' and '${app.assistente.chave:}' != ''")
    ChatClient assistenteChatClient(@Value("${app.assistente.chave}") String chave,
                                   @Value("${app.assistente.modelo}") String modelo) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        OpenAiApi api = OpenAiApi.builder().apiKey(chave)
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory)).build();
        OpenAiChatOptions options = OpenAiChatOptions.builder().model(modelo)
                .maxCompletionTokens(800).reasoningEffort("none").store(false)
                .parallelToolCalls(false).build();
        // Propaga o limite de ferramentas ao serviço em vez de devolvê-lo ao modelo,
        // evitando que ele tente novamente e gere chamadas adicionais cobradas.
        ToolCallingManager ferramentas = ToolCallingManager.builder()
                .toolExecutionExceptionProcessor(new DefaultToolExecutionExceptionProcessor(true)).build();
        // Sem repetição automática: a quota representa uma tentativa e o usuário decide se tenta de novo.
        return ChatClient.builder(OpenAiChatModel.builder().openAiApi(api)
                .defaultOptions(options).toolCallingManager(ferramentas)
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).build()).build()).build();
    }
}
