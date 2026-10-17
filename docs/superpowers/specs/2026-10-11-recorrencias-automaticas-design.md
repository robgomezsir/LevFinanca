# Recorrências automáticas — especificação de design

**Data:** 2026-10-11
**Origem:** M4-recorrencias.md §4 ("Quando gera: ao abrir o app e diariamente em segundo plano"),
aprovado em 2026-10-11 com alcance retroativo de 60 dias e teto por execução.
**Status:** aprovado para planejamento

## 1. Objetivo

Uma recorrência cadastrada (aluguel, assinatura, salário) deve se transformar sozinha em
lançamentos, sem o usuário tocar em nada:

- quando o app abre;
- todos os dias com o app fechado;
- e continuar funcionando depois que o aparelho reinicia.

A geração precisa ser **idempotente**: abrir o app dez vezes, reiniciar, ou rodar o job em
background não pode criar lançamento repetido.

## 2. O que está fora deste escopo

- Tela "A pagar / A receber" com timeline e filtro de meses (M4 §6) e o bloco "Próximos
  vencimentos" no painel (M4 §7). Esta spec só produz os dados que essas telas consumirão.
- Notificações (M8).
- Edição "só esta / esta e as futuras / todas" (M4 §3).
- Detecção de candidatas a recorrência (M4 §8).
- UI nova. Nenhuma tela é alterada.

## 3. Decisões

### 3.1 Agendamento: WorkManager

Periódico diário (`PeriodicWorkRequest`, 1 dia) para o app fechado, mais uma execução única
expedida no início do app para o efeito ser imediato. O M4 §4 autoriza WorkManager
explicitamente para esta finalidade.

Alternativas descartadas: `AlarmManager` + receiver próprio de `BOOT_COMPLETED` (mais código,
alarme inexato em API 31+ sem permissão de alarme exato, e nenhum ganho já que precisão diária
não é requisito) e materializar apenas ao abrir o app (contraria o M4 §4 e não cobre o app
fechado por dias).

**Recuperação após reinício:** vem do próprio WorkManager, que persiste o trabalho em seu banco
e o reagenda no boot via `RescheduleReceiver`. Não criamos receiver nem permissão. Esta
afirmação deve ser **verificada no manifesto mesclado** durante a implementação, não assumida.

### 3.2 Status dos lançamentos: `PENDING`

Os lançamentos automáticos entram como `status = "PENDING"`, conforme M4 §1 e §2.

Motivo: `BalanceCalculator.calculateCurrentBalance` só considera transações `PAID`. Com
`PENDING`, o saldo atual do usuário não muda por conta de uma automação, e ao mesmo tempo os
itens ficam visíveis na lista de lançamentos e prontos para a tela "A pagar". Nenhum dinheiro é
dado como gasto sem confirmação humana.

### 3.3 Janela de geração

- Para trás: `60` dias a partir de hoje.
- Para frente: `3` meses a partir de hoje (horizonte do M4 §1).
- Fechada também por `endDate` e `maxOccurrences` da recorrência.
- Teto de `200` lançamentos por execução, **mais antigos primeiro**, para que o que já venceu
  tenha prioridade sobre a previsão futura. Excedente é reportado como `truncated` no resumo e
  será materializado nas execuções seguintes.

Uma recorrência diária somada a uma janela de 60 dias pode atingir o teto: é o comportamento
esperado, não um erro.

### 3.4 Id determinístico + insert que não sobrescreve

O id do lançamento gerado é determinístico: `"rec-<recurrenceId>-<data-iso>"`.

A inserção usa `OnConflictStrategy.IGNORE` (novo método de DAO). O `insert` atual da
`TransactionDao` é `REPLACE`: com uma recorrência, um `REPLACE` apagaria um lançamento que o
usuário já confirmou (status `PAID`, valor editado) e o recriaria como `PENDING`. `IGNORE`
garante que a automação nunca desfaz trabalho do usuário.

## 4. Componentes

### 4.1 `RecurrenceMaterializer` (domain, novo, puro)

Sem dependência de Android, Room ou `LocalDate.now()` interno — `today` é parâmetro. É a única
unidade com regra de negócio, e por isso a única que precisa de teste exaustivo.

Entrada: recorrências ativas, contas, transações existentes, `today`, `nowEpochMs`, política
(`lookBackDays`, `horizonMonths`, `maxPerRun`).
Saída: `RecurrenceMaterializationPlan(transactions, skipped, truncated)` — a contagem de criados é
`transactions.size` e é derivada no chamador, não carregada como campo redundante.

`today` e `nowEpochMs` são parâmetros, não chamadas internas a `LocalDate.now()` /
`System.currentTimeMillis()`: sem isso a função não é determinística e não dá para testá-la.

Responsabilidades:

1. Gerar ocorrências via `RecurrenceEngine.generate`, ancorado em `recurrence.startDate`, com
   `endDate = min(recurrence.endDate, today + horizonMonths)`.
2. Filtrar para a janela `[today - lookBackDays, today + horizonMonths]`. O filtro vem depois da
   geração de propósito: a âncora do `RecurrenceEngine` é `startDate`, então só assim a
   ocorrência cai no dia correto do mês (31 → último dia do mês) e `maxOccurrences` continua
   contando desde o começo da série.
3. Pular ocorrências já materializadas (`recurrenceId` + `date` já presentes).
4. Pular recorrências cuja `accountId` não existe mais entre as contas ativas: o usuário
   apagou a conta, então gerar faturas para ela seria criar dados órfãos.
5. Resolver valor: `recurrence.amountCents`; se `isVariable`, o valor do lançamento mais recente
   da série (maior `date` entre as transações com aquele `recurrenceId`). **Sem histórico, a
   ocorrência é pulada** em vez de gerar valor inventado. A média prevista pelo M4 §2 foi
descartada: com o lançamento mais recente disponível a média nunca era alcançada, e ficaria
código sem uso. Valor `<= 0` também é pulado.
6. Montar `Transaction` com `status = "PENDING"`, `date = dueDate = data da ocorrência`,
   `type/accountId/categoryId/payee/notes/recurrenceId` copiados da recorrência, e
   `createdAt = updatedAt = nowEpochMs`.
7. Aplicar o teto por execução (mais antigos primeiro) e reportar truncamento.

`skipped` conta apenas ocorrências descartadas por valor ausente/inválido. Ocorrência que já
existe não entra em `skipped` — ela já é um lançamento, não um descarte.

### 4.2 `TransactionDao` — inserção idempotente

Único acréscimo: `insertIgnoringExisting(transaction)`, com
`@Insert(onConflict = OnConflictStrategy.IGNORE)`. Nenhuma consulta nova: o materializador já
recebe as transações existentes e deduplica em memória, então um `findByRecurrenceAndDate`
seria código sem uso.

### 4.3 `FinanceRepository.materializeRecurrences(today)`

Orquestra: lê recorrências e transações ativas, chama o materializador, insere cada lançamento
com `insertIgnoringExisting` e, ao final, **sincroniza as faturas dos cartões afetados**.

A sincronização de fatura é obrigatória e reusa o caminho existente em `saveTransaction`
(`syncInvoicesForAccount`). Sem ela, uma recorrência de cartão geraria lançamento com fatura
desatualizada. O novo caminho é separado do `saveTransaction` exatamente porque aquele usa
`REPLACE` e sincroniza fatura a cada inserção individual (N recargas para N lançamentos).

Devolve o resumo para log e teste.

### 4.4 `RecurrenceWorker` (novo)

`CoroutineWorker` fino: obtém o container de `(applicationContext as LevApplication).container`,
chama `materializeRecurrences()`, retorna `Result.success()`. Falha transitória → `Result.retry()`.
Não contém regra de negócio.

### 4.5 `RecurrenceScheduler` (novo)

- `ensurePeriodic(context)`: `enqueueUniquePeriodicWork("recurrence-materialization", KEEP, 1 dia)`.
  `KEEP` garante que chamar a cada início de app não duplica o job.
- `runNow(context)`: requisição única **imediata** (`OneTimeWorkRequest` simples), usada no
  início do app para o resultado aparecer sem esperar o ciclo diário.

Não usamos trabalho *expedited*: em API < 31 ele exige uma foreground service, custo
desproporcional para uma escrita local no banco. Criar/editar recorrência também não passa pelo
agendador — o `RecurrencesViewModel` chama `materializeRecurrences()` direto no repositório, o
que dá resultado imediato sem levar `Context` para dentro de um ViewModel.
- Chamado de `LevApplication.onCreate()`.

Sem `NetworkType` como constraint: a automação é local e deve rodar offline (o app é
offline-first).

## 5. Migração de banco: v2 → v3

M4 §1 exige índice único em `recurrenceId + date`:

```sql
CREATE UNIQUE INDEX IF NOT EXISTS `index_transaction_recurrenceId_date`
ON `transaction` (`recurrenceId`, `date`)
```

- Em SQLite, `NULL` não colide com `NULL` em índice único. Lançamentos sem recorrência
  (`recurrenceId IS NULL`, a esmagadora maioria) ficam intocados.
- A versão do `@Database` passa de 2 para 3, com `MIGRATION_2_3` registrada em
  `DefaultAppContainer` — o app não pode perder dados ao atualizar.
- O JSON de schema v3 precisa ser exportado (`app/schemas/`), como já ocorre para v1 e v2.
- O índice é a garantia durável; o id determinístico é a garantia prática. Os dois juntos.

## 6. Casos-limite e comportamento esperado

| Caso | Comportamento |
|---|---|
| App aberto duas vezes no mesmo dia | Nada é criado na segunda vez |
| Job diário + abertura do app no mesmo dia | Nada duplicado (mesma dedupe) |
| Reinício do aparelho | Job é reagendado pelo WorkManager; próximo ciclo roda normalmente |
| Usuário já confirmou aquela ocorrência (PAID, valor editado) | Preservada; `IGNORE` não sobrescreve |
| Recorrência variável sem histórico | Ocorrência pulada, nunca valor inventado |
| Recorrência diária antiga com muita coisa atrasada | Teto de 200, mais antigos primeiro, resto no próximo ciclo |
| Dia 31 em mês de 30 dias / fevereiro | Último dia do mês (comportamento do `RecurrenceEngine` já testado) |
| Recorrência pausada (`active = false`) | Ignorada |
| Recorrência de cartão de crédito | Lançamento criado e fatura do cartão ressincronizada |
| App fechado por mais de 60 dias | Ocorrências mais antigas que 60 dias **não** são criadas (decisão aprovada); sem aviso ao usuário nesta versão |

## 7. Testes

Testes de domínio (TDD, `app/src/test/.../domain/`), sem Android:

- janela de 60 dias: ocorrência de 61 dias atrás não é criada, de 59 dias é;
- teto por execução com prioridade para os mais antigos e `truncated` reportado;
- ocorrência já presente nas transações existentes não é recriada;
- lançamento confirmado pelo usuário (`PAID`) não é tocado;
- variável sem estimativa → pulado; variável com histórico → valor estimado;
- recorrência pausada → ignorada;
- horizonte de 3 meses respeitado;
- **executar de novo após persistir o resultado da primeira execução não cria nada** (M4 §4),
  ou seja: a segunda passada recebe o estado já atualizado;
- pureza: duas chamadas com a mesma entrada devolvem o mesmo plano (sem efeito colateral);
- `endDate` e `maxOccurrences` respeitados.

Testes de repositório (`app/src/test/.../data/repository/`, com DAOs fake no padrão de
`FinanceRepositorySyncTest`):

- `materializeRecurrences` duas vezes seguidas → mesmo resultado, sem duplicatas;
- DAO com `IGNORE` não sobrescreve lançamento existente com o mesmo id;
- conta de cartão dispara ressincronização de fatura.

Critérios de aceite (verificáveis):

1. Cadastrar um aluguel mensal e reabrir o app gera os lançamentos previstos, uma vez cada.
2. `testDebugUnitTest`, `ktlintCheck`, `detekt` e `lintDebug` passam limpos.
3. Um teste prova que a segunda execução, com os lançamentos da primeira já persistidos, não
   cria nada.

## 8. Limitações declaradas

- O `RecurrenceWorker` em si não é instrumentado (não há Robolectric/Roborazzi ligados no
  projeto). A lógica testável está no domínio e no repositório; o worker é um adaptador de
  ~10 linhas. Isso será declarado como limitação no CHANGELOG, não como cobertura.
- O `MIGRATION_2_3` não tem teste automatizado (o projeto ainda não tem
  `MigrationTestHelper`; já está em `docs/known-issues.md`).
- Atrasados além de 60 dias não são criados e não há aviso ao usuário nesta versão.

## 9. ADR

O M4 §4 exige registro. Será criado o **ADR 003 — geração automática de recorrências com
WorkManager** em `docs/decisions.md`, com contexto, decisão (WorkManager periódico diário +
execução ao abrir), alternativas descartadas (`AlarmManager`/`BOOT_COMPLETED`) e consequências
(dependência nova, atraso possível em Doze, reagendamento automático após reboot).

## 10. Arquivos afetados

Novos:

- `app/src/main/java/com/example/lev/domain/RecurrenceMaterializer.kt`
- `app/src/main/java/com/example/lev/work/RecurrenceWorker.kt`
- `app/src/main/java/com/example/lev/work/RecurrenceScheduler.kt`
- `app/src/test/java/com/example/lev/domain/RecurrenceMaterializerTest.kt`
- `app/schemas/com.example.lev.data.local.AppDatabase/3.json` (exportado pelo build)

Alterados:

- `app/build.gradle.kts` e `gradle/libs.versions.toml` (work-runtime-ktx)
- `app/src/main/java/com/example/lev/data/local/Daos.kt` (insert com `IGNORE`)
- `app/src/main/java/com/example/lev/data/local/AppDatabase.kt` (versão 3)
- `app/src/main/java/com/example/lev/data/local/Migrations.kt` (`MIGRATION_2_3`)
- `app/src/main/java/com/example/lev/data/repository/FinanceRepository.kt`
- `app/src/main/java/com/example/lev/AppContainer.kt` (registrar a migração)
- `app/src/main/java/com/example/lev/LevApplication.kt` (agendar no início)
- `app/src/main/java/com/example/lev/ui/screens/recurring/RecurrencesViewModel.kt` (disparo ao criar)
- `app/src/test/java/com/example/lev/data/repository/FinanceRepositorySyncTest.kt` (DAOs fake)
- `CHANGELOG.md`, `docs/decisions.md` (ADR 003), `docs/known-issues.md`
