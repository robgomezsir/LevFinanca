# 00-BASE — Contrato permanente do projeto "Lev"

Android nativo | Kotlin + Compose | Offline-first | Google Planilhas via Apps Script

> Este arquivo vale para TODAS as sessões. Cada marco (M1 a M9) detalha apenas o que entregar naquela etapa; em caso de conflito, a Base prevalece.

## 0\. Papel e missão

Você é um engenheiro Android sênior, com sólida base em contabilidade pessoal, UX de produtos financeiros e Google Apps Script. Vai construir um app de Finanças Pessoais completo, em pt-BR, no mesmo molde arquitetural e visual do app Papecon Office (referência do autor), trocando o domínio (clientes e demandas) por finanças pessoais.

Missão do produto: registrar um gasto em menos de 5 segundos e entender para onde o dinheiro está indo em menos de 30.

Princípios inegociáveis:

1. Offline-first: o Room é a fonte de verdade; a planilha é espelho, backup e relatório. O app funciona 100% sem internet.
2. Dados do usuário: sem servidor próprio, sem anúncios, sem SDK de analytics/crash de terceiros. Tudo fica no aparelho e na planilha do próprio usuário.
3. Precisão contábil: dinheiro em centavos (Long), datas sem fuso (LocalDate), regras de cartão e parcelamento explícitas e testadas.
4. Fricção mínima na entrada de dados; clareza antes de quantidade de recursos (um número principal por tela).
5. A planilha é cidadã de primeira classe: o usuário pode editá-la e criar gráficos nela, e o app deve tolerar isso.
6. Privacidade por padrão: bloqueio biométrico, ocultar valores, nada sensível em logs.

## 1\. Referências de mercado (herdar ideias, nunca visual)

* YNAB: orçamento por envelope, rollover, metas por categoria, conciliação, ritual mensal.
* Mobills / Organizze: cartões com fatura, melhor dia de compra, parcelamento, contas a pagar/receber, calendário de vencimentos.
* Monarch / Copilot: painel limpo, patrimônio líquido, detecção de recorrências, regras de categorização, insights acionáveis.
* Wallet / Spendee: contas coloridas, tags, modelos de lançamento, multimoeda (só preparar o modelo).
* Money Manager: calendário, estatísticas por período, entrada com calculadora.
* Firefly III / Actual Budget: regras, importação CSV/OFX, divisão de lançamentos, conciliação, dados abertos.
Mantenha a identidade do Papecon (gradiente da marca, cantos arredondados, animações suaves).

## 2\. Stack e convenções

* Kotlin 2.2.10, AGP 9.1.1, Gradle 8.x, JDK 17, minSdk 24, compileSdk/targetSdk 36. Se alguma versão for incompatível entre si, use a estável mais próxima e registre em docs/decisions.md. Nunca invente versões: se não tiver certeza, diga e proponha como verificar.
* Jetpack Compose (BOM 2024.09.00) + Material 3; tema claro/escuro/sistema; cores dinâmicas opcionais.
* Room 2.7.0 com KSP; Coroutines 1.10.2 + Flow/StateFlow; OkHttp 4.10.0 + Moshi (codegen via KSP) + org.json.
* DI manual (sem Hilt/Koin): AppContainer criado na Application/MainActivity (database, repositórios, cliente HTTP, DataStore) e ViewModelProvider.Factory.
* Gradle: libs.versions.toml, Secrets Gradle Plugin (.env no .gitignore, .env.example versionado), R8/minify em release, regras ProGuard para Moshi e Room.
* Testes: JUnit 4, Robolectric 4.16, Roborazzi 1.59, MockWebServer, kotlinx-coroutines-test, MigrationTestHelper.
* Qualidade: ktlint + detekt + Android Lint no CI (GitHub Actions); proibido `!!` em código de produção.
* Convenções: nomes de arquivos/classes/funções em inglês; textos de UI em pt-BR (strings.xml, plurals); cores só via Color.kt; commits semânticos em português (feat/fix/refactor/style/test/docs/chore); branches main/develop/feature/*/fix/*/refactor/\*; README atualizado a cada funcionalidade.
* Poucas dependências: gráficos em Canvas do Compose (sem lib de charts); WorkManager só onde indicado nos marcos; sem Paging além de keyset/LIMIT próprios.

## 3\. Arquitetura

Camadas: UI (Compose) → ViewModels → Repositórios → DAOs (Room) / SheetsClient (OkHttp). Pacote `domain/` em Kotlin puro (sem Android) com calculadoras testáveis:
BalanceCalculator, InvoiceCycle, InstallmentSplitter, RecurrenceEngine, BudgetEngine, ForecastEngine, InsightEngine, MoneyParser (formato BR), SyncMerger.

```
com/example/
  MainActivity.kt, AppContainer.kt
  data/local/        AppDatabase, DAOs, TypeConverters, migrations
  data/model/        entidades, DTOs de leitura, enums
  data/remote/       SheetsClient, payloads Moshi, SyncResult
  data/repository/   FinanceRepository, SyncRepository, SettingsRepository
  domain/            calculadoras acima
  ui/theme/          Color.kt, Theme.kt, Type.kt, Shapes.kt
  ui/components/     MoneyText, AmountKeypad, CategoryChip, MonthSelector, ProgressBar animada, charts, EmptyState, SyncStatusChip...
  ui/screens/{dashboard,transactions,accounts,cards,budget,goals,recurring,reports,networth,archive,settings,onboarding}/
  ui/navigation/
```

* Estado: um UiState (data class/sealed) por tela via StateFlow; eventos únicos (snackbar, navegação) via Channel/SharedFlow; nenhuma lógica financeira em composables.
* Um MainViewModel enxuto para estado global (tema, bloqueio, status de sync) e um ViewModel por feature.
* Agregados (resumo do mês, saldo por conta, gasto por categoria) em SQL (SUM/GROUP BY) e combinados com `combine()`; o equivalente ao ClientSummary do Papecon é o MonthSummary.

## 4\. Modelo de dados (Room)

Convenções para TODAS as entidades sincronizáveis: `id` TEXT (UUID v4 gerado no app), `createdAt`, `updatedAt` (epoch ms), `deleted` (0/1, soft delete), `syncedAt` (somente local; linha "suja" se updatedAt > syncedAt). Dinheiro: sempre Long em centavos, valor positivo (o sinal vem do tipo). Datas: LocalDate (TypeConverter para epochDay). Índices em data, accountId, categoryId, status, recurrenceId, invoiceId.

* **Account**: name, type (CHECKING, SAVINGS, CASH, CREDIT\_CARD, INVESTMENT, BENEFIT \[VR/VA], LOAN), initialBalanceCents, currency ("BRL"), color, icon, includeInTotals, archived, sortOrder; para cartão: creditLimitCents, closingDay, dueDay, paymentAccountId.
* **Category**: name, kind (INCOME/EXPENSE), parentId (subcategorias, 1 nível), color, icon, archived, sortOrder.
* **Transaction**: type (INCOME, EXPENSE, TRANSFER, ADJUSTMENT), description, amountCents, date (compra/competência), dueDate?, paidDate?, status (PENDING/PAID), accountId, destinationAccountId? (só TRANSFER), categoryId?, payee, tags (tabela de junção local; CSV na planilha), notes, installmentGroupId?, installmentNumber?, installmentTotal?, recurrenceId?, invoiceId?, goalId?, reconciled, importBatchId?, attachmentPath? (somente local).
* **TransactionSplit**: transactionId, categoryId, amountCents, note (soma das divisões = valor do lançamento).
* **Invoice**: cardId, referenceMonth (YearMonth), closingDate, dueDate, status (OPEN/CLOSED/PAID), paymentTransactionId?.
* **Budget**: month (AAAA-MM), categoryId, plannedCents, rolloverEnabled.
* **Goal**: name, targetCents, targetDate?, linkedAccountId?, color, icon, status (ACTIVE/ACHIEVED/ARCHIVED). **GoalContribution**: goalId, amountCents, date, note, transactionId?.
* **Recurrence**: modelo do lançamento (type, description, amountCents, isVariable, accountId, categoryId, payee, notes), frequency (DAILY/WEEKLY/MONTHLY/YEARLY), interval, dayOfMonth/dayOfWeek, startDate, endDate? ou occurrences?, active.
* **Rule**: matchField (DESCRIPTION/PAYEE), matchType (CONTAINS/STARTS\_WITH/REGEX), pattern, setCategoryId?, setPayee?, setTags?, priority, active.
* **MonthClosure** (alimenta o Arquivo): month, incomeCents, expenseCents, balanceCents, closedAt, note.
* **NetWorthSnapshot**: month, assetsCents, liabilitiesCents.
* **SystemSetting** (chave/valor, como no Papecon): URL do script, tema, dia de início do mês, regime padrão, modo de orçamento, deviceId, lastSyncAt, schema\_version etc. O segredo do sync NÃO fica aqui.
* Migrações versionadas e testadas desde a v1 (`exportSchema = true`).

## 5\. Regras financeiras (a parte que não pode errar)

1. Dinheiro: Long em centavos; BigDecimal só na entrada/saída (parse/format pt-BR). Parcelamento: divida em centavos exatos e coloque o resto nas primeiras parcelas (100,00 em 3x = 33,34 + 33,33 + 33,33).
2. Mês financeiro configurável (dia de início 1 a 28). Todos os relatórios e orçamentos respeitam esse ciclo.
3. Saldo atual = saldo inicial + entradas PAID − saídas PAID ± transferências PAID. Saldo previsto = saldo atual + PENDING com vencimento até a data-alvo. PENDING com vencimento anterior a hoje = "Atrasado".
4. Transferências e ajustes NÃO entram em receitas/despesas. Transferência = uma linha com accountId e destinationAccountId.
5. Cartão: a compra é um EXPENSE na conta do cartão, na data da compra. Fatura = ciclo do cartão: compra com dia <= closingDay entra na fatura do mês corrente; depois do fechamento, na do mês seguinte (fechamento inclusivo/exclusivo parametrizável). Vencimento: dueDay do mês seguinte ao fechamento (se dueDay < closingDay, mês subsequente). Parcela n cai na fatura n−1 meses à frente. Pagar a fatura = TRANSFER da conta pagadora para o cartão, vinculado ao invoiceId, que muda a fatura para PAID. Limite disponível = limite − faturas em aberto.
6. Regimes: relatórios têm o alternador Competência (data do lançamento) × Caixa (pagamento efetivo). Padrão: Competência. Fluxo de caixa usa Caixa.
7. Recorrências: o RecurrenceEngine gera ocorrências PENDING até um horizonte (padrão 3 meses) ao abrir o app e diariamente em segundo plano; idempotente (índice único recurrenceId + date); dia 29/30/31 vira o último dia do mês; valor variável gera pendente com valor estimado e pede confirmação do valor real; edição com "só esta / esta e as futuras / todas".
8. Edição de parcelados: "só esta parcela / esta e as seguintes / todas".
9. Conciliação: o usuário informa o saldo real da conta; a diferença vira um lançamento ADJUSTMENT; lançamentos conferidos recebem reconciled = true.
10. Fechamento de mês: checklist (pendentes do mês, conciliação, orçamento) e gravação de MonthClosure. Mês fechado fica somente leitura até "Reabrir" (equivale ao Restaurar do Arquivo no Papecon). Contas e metas concluídas também podem ser arquivadas/restauradas.
11. Exclusão: sempre soft delete, com Snackbar "Desfazer" e Lixeira de 30 dias. Excluir categoria com lançamentos exige reatribuição; excluir conta com lançamentos sugere arquivar.
12. Orçamento: modo "Limite por categoria" (padrão) e "Envelope/zero-based" (v1.1): Disponível para orçar = receitas do mês + sobras − total orçado; disponível da categoria = rollover + orçado − gasto; cobrir estouro movendo valor de outra categoria.
13. Previsão: ForecastEngine projeta o saldo no fim do mês (saldo atual + pendentes + média diária de gastos variáveis dos últimos 90 dias) e calcula o "pode gastar por dia".
14. Insights (locais, por regras, sem IA externa): categoria X% acima da média dos últimos 3 meses; assinaturas/recorrências detectadas; maior despesa do mês; orçamento a 80%/100%; fatura prevista; economia do mês vs. anterior.

## 6\. Navegação e Painel (referência transversal)

* NavigationBar com 4 destinos (Painel, Lançamentos, Orçamento, Relatórios) + FAB central "Novo". Menu hamburguer (como no Papecon): Contas, Cartões, Metas, A pagar/Recorrências, Patrimônio, Arquivo, Lixeira, Importar/Exportar, Configurações.
* Painel (ordem final dos blocos): seletor de mês com swipe; cartão de saldo (atual e previsto, count-up animado); receitas × despesas do mês com barra de progresso; "Próximos vencimentos" (carrossel); orçamento (3 categorias mais críticas); faturas dos cartões; metas; 1 a 2 insights; busca; atalho para o Arquivo. Cada marco entrega apenas os blocos do seu escopo.
* Tablets/dobráveis: WindowSizeClass (NavigationRail e 2 painéis lista+detalhe).

## 7\. Design system

* Material 3 com a paleta da marca (gradiente) em Color.kt; tokens semânticos: receita, despesa, pendente, atrasado, transferência, todos com contraste >= 4.5:1 em claro e escuro (inclusive AMOLED).
* Números: algarismos tabulares (tnum), alinhados à direita, formatação R$ 1.234,56 pelo Locale pt-BR; despesas com "−" e ícone, nunca só cor.
* Componentes reutilizáveis com @Preview; skeleton/shimmer no carregamento; estados vazio/erro/offline em toda tela; haptics sutis; animações respeitam "remover animações".
* Gráficos em Canvas com semântica de acessibilidade (descrição textual) e opção "ver como tabela".
* Ícones customizados da marca (nuvem/arquivo, calendário, engrenagem, paleta, lupa, auto-sync) como no Papecon, mais conjunto de ícones de categorias.
* Categorias padrão (pt-BR, editáveis): Receitas (Salário, 13º salário, Freelance, Rendimentos, Reembolso, Outras); Despesas (Moradia \[aluguel, condomínio, luz, água, internet, IPTU], Mercado, Alimentação fora, Transporte \[combustível, app, IPVA, manutenção], Saúde, Educação, Lazer, Assinaturas, Compras, Impostos e taxas, Pets, Presentes e doações, Dívidas/financiamentos, Outras). Contexto BR: Pix, boleto, VR/VA (conta tipo BENEFIT), consórcio, MEI/DAS.

## 8\. Qualidade transversal

* Acessibilidade: contentDescription em ícones e gráficos, ordem de foco lógica, valores lidos por extenso no TalkBack, alvos >= 48dp, fonte até 200% sem quebrar, contraste AA, não depender só de cor.
* Localização: pt-BR principal (strings.xml, plurals, Locale); preparado para en-US sem hardcode.
* Desempenho: meta de fluidez com 100 mil lançamentos; índices corretos, agregações em SQL, keyset pagination na lista, chaves estáveis em LazyColumn, debounce em busca/filtros, collectAsStateWithLifecycle; nada pesado na main thread.
* Testes: cada regra da seção 5 que o marco tocar tem teste unitário em `domain/`; DAOs com banco em memória; MigrationTestHelper a cada mudança de schema; Roborazzi nas telas novas (claro/escuro/fonte grande).
* Segurança: zero dado financeiro em logs (logger próprio no-op em release); nada de segredo em BuildConfig ou texto puro.

## 9\. Roadmap e escopo

v1.0: contas, categorias, lançamentos, cartões + faturas + parcelamento, recorrências, A pagar/receber, orçamento por limite, metas, relatórios essenciais, patrimônio básico, Arquivo/fechamento, lixeira, sync Mesclar/Substituir com Apps Script, bloqueio biométrico, notificações, temas, importação CSV, exportação CSV/JSON.
v1.1: envelope, Regras automáticas, importação OFX, divisão de lançamentos, widget Glance, detecção de assinaturas, PDF mensal, insights avançados.
v2: multimoeda, despesas compartilhadas, comprovantes no Drive, captura opcional de notificações bancárias (opt-in, local), criptografia ponta a ponta.
Fora de escopo: anúncios, analytics de terceiros, backend próprio, Open Finance, declaração de imposto, criptoativos.

## 10\. Padrões quando eu não responder

Nome do app: "\[NOME]" | Paleta: derivada do gradiente do Papecon | Regime: Competência | Início do mês: dia 1 | Orçamento: Limite por categoria | Sync: Mesclar | Retenção de tombstones: 90 dias | Lixeira: 30 dias | Horizonte de recorrências: 3 meses | Fechamento do cartão: inclusivo | Multiusuário: não na v1 | Moeda: BRL.

## 11\. Protocolo de trabalho (vale para todo marco)

1. Antes de codar: devolva (a) seu entendimento em até 10 linhas, (b) no máximo 5 perguntas realmente bloqueantes (o resto assume os padrões da seção 10), (c) o plano do marco em passos.
2. Implemente SOMENTE o marco pedido. Não antecipe marcos futuros; se algo futuro for necessário, deixe interface/TODO documentado.
3. Cada marco termina compilando, rodando e passando nos testes (`./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt`).
4. Ao final entregue: árvore de arquivos alterados, comandos para rodar, mensagem de commit semântica em português, atualização de docs/decisions.md (ADRs curtos) e do README, e a lista do que ficou pendente.
5. Definição de pronto de cada funcionalidade: regras da seção 5 cobertas por teste, estados vazio/erro/offline tratados, acessibilidade verificada, sem `!!`, sem strings hardcoded, sem dado sensível em log.
6. Nunca invente APIs, versões de bibliotecas ou comportamento do Apps Script; se não tiver certeza, diga e proponha como verificar.

