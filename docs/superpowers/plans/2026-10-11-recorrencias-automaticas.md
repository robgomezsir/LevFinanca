# Recorrências Automáticas — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fazer recorrências ativas virarem lançamentos sozinhas — ao abrir o app e diariamente em background — sem nunca duplicar nem sobrescrever trabalho do usuário.

**Architecture:** Toda a regra vive em `RecurrenceMaterializer` (domínio puro, sem Android). O `FinanceRepository` orquestra a persistência com insert `IGNORE` e ressincroniza faturas de cartão. Dois gatilhos chamam o mesmo método de repositório: um `CoroutineWorker` periódico diário (agendado no `LevApplication`, reagendado pelo WorkManager após reboot) e a própria criação de recorrência na UI.

**Tech Stack:** Kotlin 2.0.0, AGP 8.5.2, Room 2.7.0 + KSP, Coroutines/Flow, WorkManager 2.9.1 (`work-runtime-ktx`), JUnit 4 + `kotlinx-coroutines-test`.

**Spec:** `docs/superpowers/specs/2026-10-11-recorrencias-automaticas-design.md`

## Global Constraints

- `status = "PENDING"` em todo lançamento gerado. Nunca `PAID`: `BalanceCalculator.calculateCurrentBalance` só soma `PAID`, e uma automação não pode mexer no saldo do usuário.
- Janela: `lookBackDays = 60`, `horizonMonths = 3`, `maxPerRun = 200`, mais antigos primeiro.
- Id determinístico: `"rec-<recurrenceId>-<data-iso>"`.
- Inserção com `OnConflictStrategy.IGNORE`, nunca `REPLACE` (o `REPLACE` apagaria um lançamento já confirmado pelo usuário).
- Índice único `(recurrenceId, date)` em `transaction`, banco na versão 3, com `MIGRATION_2_3` registrada — o app não pode perder dados ao atualizar.
- Nenhuma dependência além de `androidx.work:work-runtime-ktx:2.9.1`.
- Nenhuma UI nova; nenhuma tela alterada além do gatilho em `RecurrencesViewModel`.
- Sem notificações, sem permissões novas, sem `NetworkType` como constraint (o app é offline-first).
- **Sem VCS:** este diretório não é um repositório git e não deve ser inicializado. Por isso o plano não tem passos de `git commit`; cada tarefa termina com verificação executável, e a mudança é registrada no `CHANGELOG.md` na última tarefa.
- Comandos rodam da raiz do projeto, via Git Bash. Use `set -o pipefail` ao filtrar saída do Gradle.

## Review Focus

Falhas que o spec implica mas cujos testes ninguém escreveria espontaneamente. Cada linha tem teste na tarefa dona do código:

1. **Recorrência apontando para conta apagada** → gerar fatura de conta inexistente cria dados órfãos; o esperado é pular a recorrência. (Task 1)
2. **Valor `<= 0`** (recorrência fixa zerada por edição) → criar lançamento de valor zero polui a lista; o esperado é pular. (Task 1)
3. **Ocorrência exatamente em `today`** → o limite da janela é inclusivo dos dois lados; um `isBefore`/`isAfter` trocado perde o lançamento do dia. (Task 1)
4. **`maxPerRun` exato** → com 201 ocorrências devidas, é `truncated = true` e saem 200; com 200, sai tudo e `truncated = false`. (Task 1)
5. **Duas execuções cruzando a meia-noite** (23:59 e 00:01) → a ocorrência do dia anterior já existe; nada novo para ela, nada duplicado. (Task 4)

---

### Task 1: `RecurrenceMaterializer` — janela, teto, id e dedupe

**Files:**
- Create: `app/src/main/java/com/example/lev/domain/RecurrenceMaterializer.kt`
- Test: `app/src/test/java/com/example/lev/domain/RecurrenceMaterializerTest.kt`

**Interfaces:**
- Consumes: `RecurrenceEngine.generate(...)` (existente, `app/src/main/java/com/example/lev/domain/RecurrenceEngine.kt`), `Recurrence`/`Transaction`/`Account` de `com.example.lev.data.model`.
- Produces:
  - `data class RecurrenceMaterializationPolicy(val lookBackDays: Int = 60, val horizonMonths: Int = 3, val maxPerRun: Int = 200)`
  - `data class RecurrenceMaterializationPlan(val transactions: List<Transaction>, val skipped: Int, val truncated: Boolean)`
  - `object RecurrenceMaterializer { fun buildPlan(recurrences: List<Recurrence>, accounts: List<Account>, existingTransactions: List<Transaction>, today: LocalDate, nowEpochMs: Long, policy: RecurrenceMaterializationPolicy = RecurrenceMaterializationPolicy()): RecurrenceMaterializationPlan }`

- [ ] **Step 1: Escrever os testes que falham**

No teste, um helper `recurrence(...)` devolve uma recorrência mensal fixa de `startDate` e R$ 100 (`amountCents = 10000`), e um `transaction(...)` devolve um lançamento `PAID` da série. Casos:

```kotlin
@Test fun `ocorrencia de 59 dias atras entra e de 61 dias nao`() // today = 2026-03-31, startDate = 2025-01-10
@Test fun `ocorrencia exatamente em today entra`()
@Test fun `horizonte de 3 meses e respeitado`()  // ultima data do plano <= today.plusMonths(3)
@Test fun `ocorrencia ja existente nao e recriada`()
@Test fun `lancamento confirmado pelo usuario permanece intocado no plano`()
@Test fun `recorrencia pausada e ignorada`()
@Test fun `recorrencia de conta apagada e ignorada`()   // Review Focus 1
@Test fun `valor menor ou igual a zero e pulado e conta em skipped`()  // Review Focus 2
@Test fun `teto de 200 cria 200 e marca truncated`()    // Review Focus 4b: 201 ocorrencias devidas
@Test fun `exatamente 200 ocorrencias nao marca truncated`()  // Review Focus 4b
@Test fun `mais antigos primeiro quando o teto e atingido`()
@Test fun `id e deterministico e status e PENDING`()    // id == "rec-<id>-2026-03-10", status == "PENDING", date == dueDate
@Test fun `endDate e maxOccurrences sao respeitados`()
@Test fun `duas chamadas com a mesma entrada devolvem o mesmo plano`()
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests "com.example.lev.domain.RecurrenceMaterializerTest" 2>&1 | grep -E "^e: |FAILED|BUILD"`
Expected: FAIL na compilação — `Unresolved reference: RecurrenceMaterializer`.

- [ ] **Step 3: Implementar `RecurrenceMaterializer.buildPlan`**

Ponto não óbvio que a assinatura e os testes não determinam: chame `RecurrenceEngine.generate` ancorado em `recurrence.startDate` e passando `endDate = min(recurrence.endDate, today.plusMonths(horizonMonths.toLong()))`; só depois filtre a janela `[today.minusDays(lookBackDays), today.plusMonths(horizonMonths)]`. Gerar a partir de `today` erraria o dia do mês da série; o filtro posterior preserva o clamp de dia 31 e a contagem de `maxOccurrences` desde o início da série.

Ordem final: gerar → filtrar janela → resolver valor (pular e contar em `skipped` quando ausente/`<= 0`) → remover as que já existem por `recurrenceId` + `date` → ordenar por data crescente → aplicar `maxPerRun` (o excedente vira `truncated = true`).

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests "com.example.lev.domain.RecurrenceMaterializerTest"`
Expected: PASS, 14 testes.

---

### Task 2: Estimativa de valor variável

**Files:**
- Modify: `app/src/main/java/com/example/lev/domain/RecurrenceMaterializer.kt`
- Test: `app/src/test/java/com/example/lev/domain/RecurrenceMaterializerTest.kt`

**Interfaces:**
- Consumes: `RecurrenceMaterializer.buildPlan(...)` da Task 1.
- Produces: nenhuma assinatura nova — comportamento: `isVariable = true` usa o valor do lançamento de maior `date` entre as transações com aquele `recurrenceId`.

- [ ] **Step 1: Escrever os testes que falham**

```kotlin
@Test fun `variavel sem historico e pulada e conta em skipped`()
@Test fun `variavel usa o valor do lancamento mais recente da serie`()  // 15000 mais antigo, 22000 mais recente -> 22000
@Test fun `valor da serie mais recente vence o amountCents da recorrencia`()  // amountCents = 10000, serie = 22000 -> 22000
@Test fun `variavel nao usa transacao de outra serie`()
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests "com.example.lev.domain.RecurrenceMaterializerTest" 2>&1 | grep -E "FAILED|expected"`
Expected: FAIL no valor esperado — hoje a ocorrência variável sai com `amountCents` nulo da recorrência.

- [ ] **Step 3: Implementar a resolução de valor para `isVariable`**

Ajuste a resolução de valor da Task 1: para `isVariable`, o valor vem de `existingTransactions.filter { it.recurrenceId == recurrence.id }.maxByOrNull { it.date }?.amountCents`; sem resultado, a ocorrência vai para `skipped`. Não somar médias — o M4 §2 permitia média, mas com o mais recente disponível ela nunca seria alcançada (ver spec §4.1).

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests "com.example.lev.domain.RecurrenceMaterializerTest"`
Expected: PASS, 18 testes.

---

### Task 3: Idempotência durável — DAO `IGNORE` + banco v3

**Files:**
- Modify: `app/src/main/java/com/example/lev/data/local/Daos.kt`
- Modify: `app/src/main/java/com/example/lev/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/example/lev/data/local/Migrations.kt`
- Modify: `app/src/main/java/com/example/lev/AppContainer.kt`
- Test: `app/src/test/java/com/example/lev/data/repository/FinanceRepositorySyncTest.kt` (DAOs fake já existem lá; a Task 4 usa esta fundação)

**Interfaces:**
- Produces:
  - `TransactionDao.insertIgnoringExisting(transaction: Transaction)` — `@Insert(onConflict = OnConflictStrategy.IGNORE)`
  - `AppDatabase` em `version = 3`
  - `val MIGRATION_2_3: Migration` em `com.example.lev.data.local`

- [ ] **Step 1: Adicionar `insertIgnoringExisting` à `TransactionDao`**

Mantenha o `insert` atual como está (outros fluxos usam). Não adicione consulta nova.

- [ ] **Step 2: Escrever `MIGRATION_2_3` e subir o banco para 3**

```sql
CREATE UNIQUE INDEX IF NOT EXISTS `index_transaction_recurrenceId_date`
ON `transaction` (`recurrenceId`, `date`)
```

`Migrations.kt` segue o padrão de `MIGRATION_1_2` (objeto anônimo `Migration(2, 3)` com `override fun migrate`). Em `AppContainer`, a builder passa a `.addMigrations(MIGRATION_1_2, MIGRATION_2_3)`.

- [ ] **Step 3: Compilar e confirmar o schema v3 exportado**

Run: `./gradlew compileDebugKotlin 2>&1 | grep -E "^e: |BUILD"`
Expected: BUILD SUCCESSFUL e o arquivo `app/schemas/com.example.lev.data.local.AppDatabase/3.json` criado, contendo `index_transaction_recurrenceId_date`.

- [ ] **Step 4: Confirmar que a suíte existente não regride**

Run: `set -o pipefail; ./gradlew testDebugUnitTest 2>&1 | grep -E "^e: |FAILED|BUILD"`
Expected: BUILD SUCCESSFUL, 80+ testes, 0 falhas.

---

### Task 4: `FinanceRepository.materializeRecurrences`

**Files:**
- Modify: `app/src/main/java/com/example/lev/data/repository/FinanceRepository.kt`
- Modify: `app/src/test/java/com/example/lev/data/repository/FinanceRepositorySyncTest.kt`

**Interfaces:**
- Consumes: `RecurrenceMaterializer.buildPlan(...)` (Tasks 1–2), `TransactionDao.insertIgnoringExisting` (Task 3), `syncInvoicesForAccount`/`saveInvoice` (existentes).
- Produces: `suspend fun materializeRecurrences(today: LocalDate = LocalDate.now()): RecurrenceMaterializationPlan`

- [ ] **Step 1: Escrever os testes que falham**

Nos DAOs fake já presentes no arquivo, implemente `insertIgnoringExisting` de forma que **não** altere a lista se já existir `id` igual, e registre as tentativas ignoradas para o teste poder afirmar. Casos:

```kotlin
@Test fun `materializa recorrencias ativas e nao duplica na segunda execucao`()  // 2 chamadas -> transacoes.size inalterado na 2a
@Test fun `nao sobrescreve lancamento ja existente com o mesmo id`()             // PAID do usuario continua PAID
@Test fun `execucoes cruzando a meia-noite nao duplicam a ocorrencia do dia anterior`()  // Review Focus 5
@Test fun `conta de cartao dispara ressincronizacao de fatura`()                 // saveInvoice chamado para o cartao afetado
@Test fun `conta que nao e cartao nao gera fatura`()
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests "com.example.lev.data.repository.FinanceRepositorySyncTest" 2>&1 | grep -E "^e: |FAILED"`
Expected: FAIL na compilação — `Unresolved reference: materializeRecurrences`.

- [ ] **Step 3: Implementar `materializeRecurrences`**

Ler recorrências ativas, contas ativas e transações ativas; chamar `buildPlan`; inserir cada transação com `insertIgnoringExisting`; por fim, para cada `accountId` distinto afetado que seja `CREDIT_CARD`, rechamar `syncInvoicesForAccount` com o estado já atualizado e salvar as faturas **uma vez por conta** (não por lançamento — é o motivo de este método não reusar `saveTransaction`, que sincroniza a cada inserção individual). Devolver o plano.

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests "com.example.lev.data.repository.FinanceRepositorySyncTest"`
Expected: PASS, 5 testes novos.

---

### Task 5: WorkManager — worker, agendador e ADR

**Files:**
- Modify: `gradle/libs.versions.toml`, `app/build.gradle.kts`
- Create: `app/src/main/java/com/example/lev/work/RecurrenceWorker.kt`
- Create: `app/src/main/java/com/example/lev/work/RecurrenceScheduler.kt`
- Modify: `app/src/main/java/com/example/lev/LevApplication.kt`
- Modify: `docs/decisions.md`

**Interfaces:**
- Consumes: `FinanceRepository.materializeRecurrences(...)` (Task 4), `LevApplication.container` (existente).
- Produces: `class RecurrenceWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker`, `object RecurrenceScheduler { fun ensurePeriodic(context: Context); fun runNow(context: Context); const val PERIODIC_WORK_NAME = "recurrence-materialization" }`

- [ ] **Step 1: Adicionar a dependência**

`libs.versions.toml`: versão própria `work = "2.9.1"` e library `androidx-work-runtime-ktx = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }`; em `app/build.gradle.kts`, `implementation(libs.androidx.work.runtime.ktx)`.

- [ ] **Step 2: Escrever `RecurrenceWorker`**

`doWork()` obtém o container via `(applicationContext as LevApplication).container`, chama `materializeRecurrences()` e retorna `Result.success()`; `catch (e: Exception)` → `Result.retry()`. Nenhuma regra de negócio aqui.

- [ ] **Step 3: Escrever `RecurrenceScheduler`**

`ensurePeriodic` usa `enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, PeriodicWorkRequestBuilder<RecurrenceWorker>(1, TimeUnit.DAYS).build())` — `KEEP` para o início repetido do app não criar jobs duplicados. `runNow` enfileira um `OneTimeWorkRequest` simples com o mesmo nome único; **não** usar `setExpedited` (em API < 31 exigiria foreground service, ver spec §4.5). Operadores em `BackgroundThread`/`Executor` não são necessários: estas funções só enfileiram.

- [ ] **Step 4: Agendar no início do app**

`LevApplication.onCreate()` chama `RecurrenceScheduler.ensurePeriodic(this)` e `runNow(this)` depois de criar o container.

- [ ] **Step 5: Verificar que o reboot é coberto pelo WorkManager (não assumir)**

Run: `./gradlew assembleDebug 2>&1 | grep -E "^e: |BUILD"` e depois
`grep -o 'androidx.work.impl.background.systemalarm.RescheduleReceiver\|RECEIVE_BOOT_COMPLETED\|SystemJobService' app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml | sort -u`
Expected: BUILD SUCCESSFUL e as três entradas presentes no manifesto mesclado. Se `RescheduleReceiver` não aparecer, o reagendamento pós-reboot **não** está garantido e este passo falhou — investigar antes de seguir.

- [ ] **Step 6: Registrar o ADR 003 em `docs/decisions.md`**

ADR com contexto (M4 §4 exige), decisão (WorkManager periódico diário + execução imediata no início; `KEEP`; sem `NetworkType`), alternativas descartadas (`AlarmManager` + `BOOT_COMPLETED`; só ao abrir) e consequências (dependência nova, atraso possível em Doze, reagendamento automático no boot verificado no manifesto mesclado, sem permissão nova).

- [ ] **Step 7: Verificar**

Run: `set -o pipefail; ./gradlew ktlintCheck detekt assembleDebug 2>&1 | grep -E "^e: |FAILED|BUILD"`
Expected: BUILD SUCCESSFUL.

---

### Task 6: Gatilho na criação de recorrência + documentação

**Files:**
- Modify: `app/src/main/java/com/example/lev/ui/screens/recurring/RecurrencesViewModel.kt`
- Modify: `CHANGELOG.md`
- Modify: `docs/known-issues.md`

**Interfaces:**
- Consumes: `FinanceRepository.materializeRecurrences(...)` (Task 4), `FinanceRepository.saveRecurrence`/`updateRecurrence` (existentes).
- Produces: comportamento — criar ou alternar uma recorrência materializa na hora, sem esperar o job diário.

- [ ] **Step 1: Escrever o teste que falha**

Em `app/src/test/java/com/example/lev/ui/screens/recurring/RecurrencesViewModelTest.kt` (novo), com DAOs fake que já contêm uma recorrência mensal ativa com uma ocorrência vencida (dentro dos 60 dias):

```kotlin
@Test fun `criar uma recorrencia materializa tambem as vencidas que ja existiam`()
```

A asserção é o total de transações do DAO fake depois de `createRecurrence(...)` — ele precisa incluir a ocorrência da recorrência pré-existente, provando que o ViewModel chamou a materialização. Use `runTest` com `Dispatchers.setMain(StandardTestDispatcher())` (padrão `kotlinx-coroutines-test` já usado no projeto). Se o harness de Main dispatcher exigir configuração que o projeto não tem, **pare e reporte** em vez de mockar em volta.

- [ ] **Step 2: Rodar e ver falhar**

Run: `./gradlew testDebugUnitTest --tests "com.example.lev.ui.screens.recurring.RecurrencesViewModelTest" 2>&1 | grep -E "^e: |FAILED|expected"`
Expected: FAIL na asserção — o total é 1 (só o lançamento da recorrência nova salvando), não 2.

- [ ] **Step 3: Disparar a materialização no ViewModel**

Em `createRecurrence`, depois de `saveRecurrence`, e em `toggleActive`, depois de `updateRecurrence`, chamar `financeRepository.materializeRecurrences()` dentro do mesmo `viewModelScope.launch`. Sem `Context`: o agendador não entra aqui (spec §4.5).

- [ ] **Step 4: Rodar e ver passar**

Run: `./gradlew testDebugUnitTest --tests "com.example.lev.ui.screens.recurring.RecurrencesViewModelTest"`
Expected: PASS.

- [ ] **Step 5: Atualizar a documentação**

`CHANGELOG.md` → `### Added`: recorrências materializadas automaticamente (WorkManager diário + ao abrir), com a limitação declarada de que o `RecurrenceWorker` em si não é instrumentado e que `MIGRATION_2_3` não tem teste automatizado. `docs/known-issues.md`: remover a linha "planejar geração automática de recorrências vencidas via WorkManager".

- [ ] **Step 6: Verificação final**

Run: `set -o pipefail; ./gradlew testDebugUnitTest lintDebug ktlintCheck detekt assembleDebug 2>&1 | grep -E "^e: |Error:|FAILED|BUILD|Lint found"`
Expected: BUILD SUCCESSFUL, 0 erros de lint, nenhuma violação de ktlint/detekt, suíte verde.
