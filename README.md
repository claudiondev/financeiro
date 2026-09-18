
# 💰 Meu Controle Financeiro — API Backend

![Java](https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.15-green?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6.4-green?style=for-the-badge&logo=springsecurity)
![JWT](https://img.shields.io/badge/JWT-Auth-black?style=for-the-badge&logo=jsonwebtokens)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue?style=for-the-badge&logo=mysql)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?style=for-the-badge&logo=flyway)
![Render](https://img.shields.io/badge/Render-Deploy-46E3B7?style=for-the-badge&logo=render)

> API REST para controle financeiro pessoal: autenticação JWT, orçamentos, contas recorrentes, importação OFX, metas de economia, insights determinísticos e chat financeiro opcional com IA.

---

## 🔗 API Online

🚀 **Acesse:** https://financeiro-lk6d.onrender.com

> ⚠️ A rota raiz retorna 403 por segurança — use os endpoints abaixo (`/auth`, `/gastos`, etc.) para testar.
> ⏳ Hospedado no tier gratuito do Render: após ~15 min sem uso o servidor "dorme" e o primeiro request pode levar até 2 min pra responder (cold start).

Quer ver os dados sem criar conta? `POST /auth/demo` devolve um token válido direto, sem senha, para uma conta compartilhada de demonstração (somente leitura — ver seção **Modo Demo** abaixo). O mesmo fluxo tem um botão pronto no [front-end](https://meu-financeiro-pessoal.vercel.app).

---

## 📋 Sobre o Projeto

O Meu Controle Financeiro é o backend de uma aplicação full stack de controle financeiro pessoal: gastos, salários, metas de orçamento, contas recorrentes e um assistente que avisa quando algo foge do planejado.

Construído com Java e Spring Boot, seguindo arquitetura em camadas com DTOs de entrada/saída, exceptions centralizadas, migrações de banco versionadas com Flyway e cobertura de testes (unitários + integração). Hospedado em produção no **Render**, com banco **TiDB Serverless** (compatível com MySQL).

---

## ✨ Funcionalidades

- 🔐 **Autenticação JWT completa** — cadastro, login, recuperação de senha por e-mail (código com expiração de 15 min), política de senha forte (8+ caracteres, maiúscula, número) e revogação automática de token ao trocar a senha
- 🎭 **Modo demo** — `POST /auth/demo` libera uma conta compartilhada com dados de exemplo realistas, sem senha e sem cadastro; qualquer escrita nessa conta é bloqueada no servidor (`423 Locked`), não só na interface
- 💰 **Gastos** — CRUD completo com categoria fixa (enum), forma de pagamento e parcelamento no cartão de crédito (divide o valor automaticamente, sem perder centavo no arredondamento)
- 💼 **Salários** — CRUD com valor, comissão e adicional, filtrável por mês
- 🎯 **Metas de orçamento** — limite mensal por categoria, com cálculo automático de consumo e status (dentro do limite / atenção / estourado)
- 🔁 **Contas fixas recorrentes e financiamentos** — gera o gasto do mês automaticamente, status (pago / vencendo / atrasado / pendente), pausar/reativar sem perder o histórico, lembrete por e-mail opcional; com número de parcelas definido vira um financiamento (para de gerar sozinho ao chegar no total, saldo devedor calculado ao vivo)
- 🎯 **Metas de economia (poupança)** — meta de valor-alvo com prazo opcional; "registrar aporte" cria um gasto na categoria Poupança, progresso sempre calculado ao vivo (nunca armazenado), status EM_ANDAMENTO/CONCLUÍDA/ATRASADA
- 📥 **Importação de extrato bancário (OFX)** — parser próprio (sem lib externa) para OFX 1.x/2.x, fluxo em 2 passos com revisão de categoria antes de confirmar, dedup por FITID (evita duplicar transação já importada)
- 🤖 **Assistente financeiro** — motor de regras (não depende de LLM) que cruza orçamentos, ritmo de gastos, variação por categoria e dicas educacionais em insights priorizados por severidade
- 💬 **Chat financeiro opcional** — Spring AI com três consultas de leitura sobre gastos, períodos e orçamentos; sessões temporárias isoladas, quota diária e chave de API apenas no servidor
- 📈 **Evolução mensal** — série histórica de entradas, saídas e saldo
- 📊 **Resumo e relatório** — saldo do mês, gasto por categoria, transações recentes
- 🛡️ **Segurança em produção** — rate limiting no login/recuperação, headers HSTS/CSP/X-Frame-Options, proteção contra IDOR (todo endpoint filtra por dono do recurso), senha com BCrypt, revogação de token ao trocar senha
- 🗄️ **16 migrações versionadas** com Flyway (schema evoluído incrementalmente, sem `ddl-auto=update`)
- ✅ **Mais de 150 testes automatizados** — JUnit 5 + Mockito nos services, testes de integração com MockMvc

---

## 🔗 Principais Endpoints

### 🔐 Autenticação (`/auth`)
- `POST /auth/registrar` → Cadastro de usuário
- `POST /auth/login` → Login e geração de token JWT
- `POST /auth/demo` → Token da conta demo, sem senha
- `POST /auth/recuperar-senha` → Envia código de recuperação por e-mail
- `POST /auth/redefinir-senha` → Troca a senha com o código recebido

### 👤 Usuário (`/usuario`)
- `GET /usuario/me` → Dados do usuário logado (nome, e-mail, se é conta demo)
- `PUT /usuario/perfil` → Atualiza o nome

### 💰 Gastos (`/gastos`)
- `POST /gastos` → Criar gasto (parcelado ou não)
- `GET /gastos/filtrar?mes=&ano=&categoria=` → Listar com filtros
- `PUT /gastos/{id}` → Editar
- `DELETE /gastos/{id}` → Remover (bloqueado se for parcela avulsa)
- `DELETE /gastos/{id}/parcelamento` → Remove a compra parcelada inteira
- `PATCH /gastos/{id}/pagar` → Marca uma conta fixa gerada como paga
- `GET /gastos/resumo` · `/relatorio` · `/categorias` · `/parcelamentos` · `/evolucao?meses=N`

### 💼 Salários (`/salario`)
- `POST /salario` · `GET /salario/filtrar?mes=&ano=` · `PUT /salario/{id}` · `DELETE /salario/{id}`

### 🎯 Metas de Orçamento (`/orcamentos`)
- `POST /orcamentos` → Cria ou atualiza o limite da categoria (upsert)
- `GET /orcamentos` → Lista com consumo do mês corrente
- `DELETE /orcamentos/{id}`

### 🔁 Contas Fixas e Financiamentos (`/gastos-fixos`)
- `POST /gastos-fixos` → Cria (com `totalParcelas` opcional vira financiamento) · `GET /gastos-fixos` · `PUT /gastos-fixos/{id}` · `DELETE /gastos-fixos/{id}`
- `PATCH /gastos-fixos/{id}/pausar` · `/reativar`
- `GET /gastos-fixos/pendentes-alerta`

### 🎯 Metas de Economia (`/metas-economia`)
- `POST /metas-economia` · `GET /metas-economia` · `PUT /metas-economia/{id}` · `DELETE /metas-economia/{id}`
- `POST /metas-economia/{id}/aportes` → Registra aporte (cria um gasto na categoria Poupança vinculado à meta)

### 📥 Importação de Extrato (`/importacao`)
- `POST /importacao/ofx` → Lê o arquivo OFX e devolve a lista de transações para revisão (nada é salvo ainda)
- `POST /importacao/confirmar` → Recebe os itens revisados/marcados e cria os `Gasto`/`Salario` de fato

### 🤖 Assistente (`/assistente`)
- `GET /assistente/insights` → Top 5 insights do mês, por severidade
- `GET /assistente/chat/status` → Informa se o chat está habilitado
- `POST /assistente/chat/sessoes` → Abre uma sessão temporária e devolve seu segredo
- `POST /assistente/chat/mensagens` → Recebe `{ "mensagem": "...", "requisicaoId": "UUID" }` com header `X-Assistente-Sessao`
- `DELETE /assistente/chat/sessao` → Encerra a sessão identificada pelo mesmo header

O chat fica desligado por padrão. Para ativá-lo, configure `ASSISTENTE_HABILITADO=true` e `OPENAI_API_KEY` no ambiente do servidor. `ASSISTENTE_MODELO` é opcional (padrão `gpt-5.6-luna`). As perguntas e os dados financeiros necessários à resposta são processados pela OpenAI. As conversas ficam apenas na memória do backend e expiram após 30 minutos sem atividade. A demo usa sessões separadas por visitante; nenhuma ferramenta altera lançamentos. O limite diário inicial é de 30 perguntas para a demo, 20 por conta normal e 50 no aplicativo inteiro.

Em produção, aplique `V17__create_quota_assistente.sql` manualmente no TiDB Cloud **antes** de ativar o chat. O Flyway está desabilitado nesse ambiente por causa da incompatibilidade do `GET_LOCK()` com o gateway do TiDB.

---

## 🧪 Testes

```bash
./mvnw test
```

Os testes cobrem services, parser de OFX, autenticação, isolamento de dados, quota concorrente e inicialização do chat com ou sem chave, sem fazer chamadas reais à OpenAI.

---

## 📁 Estrutura de Pacotes

```
src/main/java/com/claudio/financeiro
├── controller   # Endpoints da API (requisições HTTP)
├── service      # Regras de negócio
├── repository   # Acesso ao banco de dados (Spring Data JPA)
├── model        # Entidades JPA e enums de domínio
├── dto          # Objetos de entrada/saída — controllers nunca expõem entidade direto
├── exception    # GlobalExceptionHandler — erros padronizados em {"erro": "..."}
└── config       # Segurança (JWT, Spring Security), rate limiting, seed do modo demo

src/main/resources/db/migration   # Migrações Flyway (V2 a V17)
```

---

## 🏗️ Arquitetura

Camadas clássicas (`Controller → Service → Repository`), com alguns pontos que valem destacar:

- **DTOs de entrada e saída** em todo endpoint novo — nenhuma entidade JPA trafega direto no `@RequestBody`/`@ResponseBody` dos recursos principais
- **Services decompostos por responsabilidade**: `GastoService` foi quebrado em `RelatorioService`, `EvolucaoService` e `CalculoFinanceiroUtil` quando cresceu demais, em vez de virar um God Class
- **`GlobalExceptionHandler`** centraliza erros de validação, ownership (403/404) e JSON inválido num formato único
- **Insights determinísticos atrás de uma interface** (`GeradorDeInsight`) e chat com IA em serviço separado — os insights funcionam sem chave da OpenAI
- **Valores monetários em `BigDecimal`**, nunca `Double` — evita erro de arredondamento em soma/parcelamento
- **Progresso derivado, nunca armazenado** — parcelas pagas de um financiamento e valor acumulado de uma meta de economia são sempre a soma ao vivo dos registros vinculados, não um contador salvo que pode dessincronizar
- **`OfxParser` sem dependência externa** — parser próprio via regex, tolerante ao OFX 1.x (SGML solto, tags sem fechamento) que a maioria dos bancos brasileiros exporta, além do XML fechado do OFX 2.x
- **Migrações Flyway incrementais**, com estratégia *expand-and-contract* nas mudanças que trocam o tipo de uma coluna existente

---

## 🚀 Como Rodar Localmente

### Pré-requisitos
- Java 17
- MySQL rodando localmente (ou ajuste `application-dev.properties` para outro banco)

### Passos

```bash
# Clone o repositório
git clone https://github.com/claudiondev/financeiro
cd financeiro

# Configure as variáveis de ambiente
cp .env.example .env
# Preencha DB_USERNAME, DB_PASSWORD, JWT_SECRET (32+ caracteres) e, se quiser
# testar recuperação de senha/lembretes, MAIL_USERNAME/MAIL_PASSWORD

# Rode com o profile de desenvolvimento
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A API sobe em `http://localhost:8080`. No primeiro boot, o Flyway aplica as migrações e uma conta demo com dados de exemplo é criada automaticamente (`demo@meufinanceiro.app`, sem senha — use `POST /auth/demo`).

---

## 🚀 Tecnologias Utilizadas

| Tecnologia | Descrição |
|---|---|
| Java 17 | Linguagem principal do projeto |
| Spring Boot 3.5.15 | Framework para construção da API |
| Spring AI 1.1.8 | Integração opcional com o modelo e ferramentas de consulta |
| Spring Security | Autenticação stateless via JWT, headers de segurança |
| JJWT 0.12.6 | Geração e validação de tokens JWT |
| Spring Data JPA | Comunicação com banco de dados |
| Flyway | Versionamento e migração de schema |
| MySQL / TiDB Serverless | Banco relacional (local: MySQL · produção: TiDB) |
| Lombok | Redução de boilerplate nos DTOs |
| JavaMailSender | Recuperação de senha e lembretes de conta fixa |
| JUnit 5 + Mockito | Testes unitários e de integração |
| Render | Deploy da aplicação |

---
## 📌 Autor

**Claudio Nascimento**
🔗 https://github.com/claudiondev
