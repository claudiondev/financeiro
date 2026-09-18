# Chat financeiro com IA — desenho da primeira versão

## Objetivo

Permitir perguntas em português sobre gastos, entradas e orçamentos já registrados, mantendo os insights determinísticos. A primeira versão atende ao portfólio com uma integração real de ferramentas sem permitir alterações financeiras pelo modelo.

## Fluxo

1. O frontend consulta `GET /assistente/chat/status` e mostra o chat apenas quando há configuração válida.
2. Após o aviso sobre processamento pela OpenAI, cria uma sessão em `POST /assistente/chat/sessoes`.
3. Cada pergunta é enviada a `POST /assistente/chat/mensagens` com um UUID de requisição e o segredo de sessão no header `X-Assistente-Sessao`.
4. O backend valida JWT, sessão, ritmo e quota antes de chamar o modelo. O modelo dispõe de três ferramentas de leitura e recebe `usuarioId` somente pelo `ToolContext` do servidor.
5. Apenas pares completos de pergunta e resposta entram no histórico temporário. `DELETE /assistente/chat/sessao` encerra a conversa.

## Dados e cálculos

As ferramentas consultam resumo por período, gastos por período e orçamentos de um mês. Os totais são calculados no Java em `BigDecimal`. Apenas gastos pagos contam como saída realizada. As consultas não chamam o gerador de gastos fixos, que pode escrever no banco. Listas de gastos exibem até 20 itens, mas o total considera todos os registros do período. O período máximo é de 12 meses.

O saldo é a diferença entre entradas e saídas registradas, não o saldo bancário. Orçamentos usam os limites cadastrados atualmente, mesmo quando o usuário pergunta sobre meses passados. Essas limitações são informadas ao modelo.

## Sessões, custo e falhas

Conversas ficam em memória por até 30 minutos sem atividade, com até dez mensagens no contexto. Cada sessão tem segredo aleatório próprio; visitantes da conta demo não compartilham o histórico. Reenvio com o mesmo UUID durante a sessão devolve a resposta anterior. Não há persistência de conversas.

A quota diária fica no banco (`V17`): 30 perguntas compartilhadas na demo, 20 por conta normal e 50 globais. A atualização SQL do contador é atômica. Cada sessão aceita até cinco perguntas por minuto, e no máximo três chamadas de ferramentas por pergunta. O modelo tem limite de 800 tokens de saída por chamada e timeout de leitura de 45 segundos, sem repetição automática. A quota é reservada antes da chamada externa, inclusive quando o provedor falha.

Sem chave ou com a funcionalidade desabilitada, os insights continuam disponíveis. Falhas do provedor retornam mensagem genérica ao cliente; prompts e respostas não são registrados em log.

## Segurança e privacidade

O usuário das ferramentas vem do JWT e não pode ser escolhido pelo modelo. Os resultados das ferramentas contêm somente os campos necessários. Descrições de lançamentos são tratadas como dados não confiáveis. O chat responde a questões financeiras pessoais, sem recomendar ativos específicos nem afirmar taxas atuais sem fonte.

O frontend informa antes do primeiro envio que perguntas e dados necessários serão processados pela OpenAI. Conversas temporárias reduzem retenção no banco da aplicação; a retenção do provedor segue a política da API usada.

## Entrega

Backend: Spring Boot 3.5.15, Spring AI 1.1.8, consultas, ferramentas, chat, quota e testes. Frontend: chat na página Assistente, mantendo os insights. Produção: aplicar V17 no TiDB antes de ativar `ASSISTENTE_HABILITADO` e `OPENAI_API_KEY` no Render. A ativação é separada do código porque o Flyway fica desabilitado em produção.
