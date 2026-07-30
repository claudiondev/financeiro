# Gastos Fixos / Contas Fixas — Design

> Spec aprovada em 30/07/2026, via sessão de brainstorming com o Claudio. Cobre só a primeira das duas funcionalidades pedidas (gastos fixos/recorrentes) — forma de pagamento + parcelamento fica pra uma spec separada, depois desta implementada.

## Contexto e motivação

Hoje todo `Gasto` é lançado manualmente. Contas que se repetem todo mês (aluguel, assinatura, internet) obrigam o usuário a recadastrar o mesmo lançamento mês a mês. Além disso, não existe noção de "conta pendente de pagamento" — todo gasto lançado já é tratado como dinheiro que saiu na hora.

Esta spec resolve os dois problemas juntos, porque são a mesma decisão de modelagem: uma conta fixa nasce **pendente** todo mês até o usuário confirmar que pagou.

## Modelo de dados

### `GastoFixo` (nova entidade — o "molde" da conta recorrente)

| Campo | Tipo | Observação |
|---|---|---|
| `id` | Long | PK, identity |
| `usuario` | Usuario | FK, dono do gasto fixo |
| `categoria` | CategoriaGasto | reaproveita o enum existente |
| `valor` | BigDecimal | `DECIMAL(12,2)`, valor de cada ocorrência mensal |
| `descricao` | String | ex: "Aluguel", "Internet Vivo Fibra" |
| `diaVencimento` | Integer | 1–31. Validação `@Min(1) @Max(31)` |
| `dataInicio` | LocalDate | a partir de qual mês/ano passa a gerar (evita gerar retroativo) |
| `ativo` | boolean | default `true`. Pausar = `false`, sem apagar histórico já gerado |

Índice único recomendado: nenhum — o usuário pode ter duas contas fixas com mesma categoria/valor (ex: dois aluguéis de imóveis diferentes), então não há chave natural de unicidade além do `id`.

### `Gasto` (existente) — dois campos novos

| Campo | Tipo | Observação |
|---|---|---|
| `pago` | boolean | `NOT NULL DEFAULT TRUE`. Gasto avulso criado manualmente sempre nasce `true` (comportamento atual, inalterado). Gasto gerado a partir de um `GastoFixo` nasce `false`. |
| `gastoFixo` | GastoFixo (nullable) | FK opcional. `null` para gastos avulsos. Aponta pro molde de origem quando o gasto foi auto-gerado. |

### Migrations Flyway

- `V6__create_gastos_fixos_table.sql` — cria a tabela `gastos_fixos` (colunas acima, FK pra `usuarios`)
- `V7__add_pago_e_gasto_fixo_id_em_gastos.sql`:
  - `ALTER TABLE gastos ADD COLUMN pago BOOLEAN NOT NULL DEFAULT TRUE`
  - `ADD COLUMN gasto_fixo_id BIGINT NULL`
  - `ADD CONSTRAINT fk_gasto_gasto_fixo FOREIGN KEY (gasto_fixo_id) REFERENCES gastos_fixos(id) ON DELETE SET NULL` — se o molde for apagado, o histórico gerado por ele não é apagado junto, só perde a referência
  - `ADD COLUMN ultimo_lembrete_enviado_em DATE NULL` — controla o e-mail de lembrete (ver seção "Lembrete de pagamento")

## Geração automática (sem rotina agendada)

Motivo: o backend não roda 24/7 hoje (Railway desativado, uso é local/sob demanda). Uma rotina `@Scheduled` fixa (ex: todo dia 1 às 00h) não é confiável nesse cenário — se o servidor não estiver de pé naquele instante exato, a geração nunca acontece.

**Solução: geração "preguiçosa" (lazy), verificada em toda leitura de gastos de um mês.**

Novo método `GastoFixoService.garantirGastosDoMesGerados(usuarioId, mes, ano)`:
1. Busca `GastoFixo` do usuário com `ativo=true` e `dataInicio <= primeiroDiaDoMes(mes, ano)`
2. Para cada um, verifica via `GastoRepository.existsByGastoFixoIdAndMesEAno(...)` se já existe um `Gasto` gerado dele pra esse mês/ano
3. Se não existir, cria um `Gasto` novo: `valor`/`categoria`/`descricao` copiados do molde, `pago=false`, `gastoFixo` apontando pro molde, `data` = `diaVencimento` daquele mês/ano — se o mês não tiver esse dia (ex: dia 31 em fevereiro), usa o último dia do mês (`YearMonth.atEndOfMonth()`)

Esse método é chamado no início de: `GastoService.buscarGastosDoMes` (usado por `/gastos/filtrar`), `GastoService.calcularResumo` (usado por `/gastos/resumo`), e `GastoService.getEvolucaoMensal` (pra cada mês do período). Idempotente — chamar de novo no mesmo mês não duplica nada, graças ao passo 2.

## Saldo passa a contar só o que foi pago

Todos os pontos que hoje somam `Gasto.valor` do mês passam a filtrar `pago=true`:
- `GastoService.calcularResumo` (saldo, total de gastos, gráfico de categorias)
- `GastoService.getRelatorio`
- `GastoService.getEvolucaoMensal`
- `OrcamentoService.calcularConsumoDaCategoria` (consumo do orçamento)

Gasto avulso continua contando na hora (nasce `pago=true`), então esse filtro é transparente pro comportamento atual — só passa a excluir os gastos fixos ainda não confirmados.

## Lembrete de pagamento

**No app**: endpoint novo `GET /gastos-fixos/pendentes-alerta` retorna os `GastoFixo` ativos cujo gasto do mês corrente está `pago=false` e vence em ≤ 3 dias ou já venceu. O `Resumo.jsx` chama esse endpoint e, se a lista não for vazia, mostra um aviso compacto (não um card grande) tipo "⚠ 2 contas vencendo esta semana", linkando pra `/contas-fixas`.

**E-mail**: disparado a partir do mesmo endpoint/lógica, no momento em que o usuário acessa `/gastos/resumo` (ou faz login) — não em rotina agendada, mesmo raciocínio da geração automática. Pra não enviar toda hora que o usuário navega, usa a coluna `ultimo_lembrete_enviado_em` (ver migration `V7` acima) na própria linha do `Gasto` gerado — só envia e-mail se essa data for `null` ou diferente de hoje. Assunto: "Conta a vencer: {descricao}". Reaproveita `spring-boot-starter-mail` já configurado (usado hoje só pra recuperação de senha).

**Definição precisa do `statusMesAtual`** (usado tanto no endpoint `GET /gastos-fixos` quanto no alerta/e-mail — mesmo limiar em todo lugar, sem número mágico duplicado):
- `PAGO` — `pago=true`
- `ATRASADO` — `pago=false` e `data < hoje`
- `VENCENDO` — `pago=false` e `hoje <= data <= hoje + 3 dias`
- `PENDENTE` — `pago=false` e `data > hoje + 3 dias`

O alerta no Resumo e o e-mail disparam pra `VENCENDO` e `ATRASADO`; `PENDENTE` só aparece na tela de Contas Fixas, sem alarde.

## Backend — endpoints novos

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/gastos-fixos` | Cria um novo `GastoFixo` |
| `GET` | `/gastos-fixos` | Lista os `GastoFixo` do usuário, com status do mês corrente embutido (pago/pendente/vencendo/atrasado) |
| `PUT` | `/gastos-fixos/{id}` | Edita um `GastoFixo` (mesmo padrão DTO de entrada + ownership já usado em Gasto/Salario/Orcamento) |
| `DELETE` | `/gastos-fixos/{id}` | Remove o molde. Gastos já gerados a partir dele **permanecem** (histórico não é apagado) — a FK `ON DELETE SET NULL` (definida na migration `V7`) garante isso automaticamente |
| `PATCH` | `/gastos/{id}/pagar` | Marca um `Gasto` (gerado de conta fixa) como `pago=true`. Endpoint novo e específico em vez de reaproveitar o `PUT` genérico, porque só faz sentido mudar esse campo isoladamente |
| `GET` | `/gastos-fixos/pendentes-alerta` | Usado pelo aviso no Resumo e pelo disparo de e-mail (ver acima) |

DTOs novos: `CriarGastoFixoRequest`, `GastoFixoDTO` (inclui campo calculado `statusMesAtual`: `PAGO` / `PENDENTE` / `VENCENDO` / `ATRASADO`).

## Frontend

- Página nova `pages/ContasFixas/ContasFixas.jsx`: lista os gastos fixos com badge de status (mesma paleta semântica já usada em Metas: verde/laranja/vermelho), botão de marcar como pago, modal de criar/editar (mesmo padrão CRUD já usado em Metas/Gastos/Salários)
- Item novo no Sidebar: "Contas Fixas", ícone `Repeat` (lucide-react), entre "Salário" e "Metas"
- Rota `/contas-fixas` no `App.jsx`
- `Resumo.jsx`: novo aviso compacto condicional (só renderiza se a lista de `pendentes-alerta` não for vazia)

## Testes

Backend (padrão Mockito já usado no projeto):
- `GastoFixoServiceTest`: geração idempotente (chamar duas vezes no mesmo mês não duplica), ajuste de dia em mês curto (dia 31 → 28/29 em fevereiro), `dataInicio` no futuro não gera nada ainda, `ativo=false` não gera
- `GastoServiceTest`: saldo/resumo/relatório/evolução somam só `pago=true` (ajusta os testes existentes que hoje assumem todo gasto contando)
- `OrcamentoServiceTest`: consumo do orçamento não conta gasto fixo pendente
- Teste do disparo de e-mail com `@MockBean` de `JavaMailSender` (mesmo padrão já usado em `FinanceiroApplicationTests`)

## Fora de escopo desta spec

- Forma de pagamento (cartão/dinheiro/Pix) e parcelamento — spec separada, depois desta
- Data de término opcional pro gasto fixo (contrato por tempo determinado) — decidido ficar de fora por ora; `ativo=false` (pausar manualmente) cobre o caso por enquanto
- Notificação push/SMS — só e-mail nesta rodada
- Editar um gasto fixo não altera retroativamente gastos já gerados em meses passados, só os futuros
