# M1 — Fundação: projeto, CI, tema, Room, migrações, seeds, navegação esqueleto

**Pré-requisito:** 00-BASE.md lido. Repositório vazio.
**Objetivo:** deixar o esqueleto do app compilando, com banco completo, tema da marca, navegação vazia e CI verde. Nenhuma regra de negócio ainda.

## Escopo
1. **Projeto Gradle**: libs.versions.toml (versões da Base §2), KSP, Secrets Gradle Plugin (.env/.env.example), R8 + ProGuard (Moshi, Room), .gitignore, gradle.properties, wrapper. Pacote `com.example`, applicationId a definir (pergunte ou use `com.example.financas`).
2. **CI** (GitHub Actions): ktlint, detekt, Android Lint, `testDebugUnitTest`, `assembleDebug`. Configuração do detekt proibindo `!!`.
3. **AppContainer + MainActivity** com DI manual e ViewModelProvider.Factory genérica; Application class.
4. **Tema**: `Color.kt` (gradiente da marca + tokens semânticos: receita, despesa, pendente, atrasado, transferência; claro, escuro e AMOLED), `Theme.kt` (claro/escuro/sistema, cores dinâmicas opcionais), `Type.kt` (algarismos tabulares nos estilos numéricos), `Shapes.kt` (cantos arredondados).
5. **Domain base**: `MoneyParser` (aceita `R$ 1.234,56`, `1234,56`, `1234.56`, `1.234`, vazio) e `MoneyFormatter` pt-BR em centavos, ambos em Kotlin puro.
6. **Componentes base** com @Preview: `MoneyText`, `EmptyState`, `Skeleton/shimmer`, `SyncStatusChip` (só visual), `ProgressBar` animada.
7. **Room (schema v1 completo)**: todas as entidades da Base §4 (inclusive as de marcos futuros, para evitar migração prematura), TypeConverters (LocalDate, YearMonth, enums), DAOs com CRUD básico e consultas de soft delete (`deleted = 0`), índices, FKs com ON DELETE explícito, `exportSchema = true`, tabela de junção de tags.
8. **Seeds** idempotentes: categorias padrão (Base §7) e SystemSettings default (Base §10). Sem conta padrão.
9. **Navegação esqueleto**: NavigationBar com 4 destinos + FAB central + drawer com os itens da Base §6, todas as telas como placeholders com EmptyState; suporte a WindowSizeClass (NavigationRail em telas largas).
10. **Docs**: README inicial (formato do Papecon: badges, índice, stack, arquitetura, modelo de dados), `docs/decisions.md` com ADR de versões e de "schema v1 completo".
11. **Gerador de dados de teste** (somente build debug): cria N contas e até 100 mil lançamentos aleatórios para testes de desempenho nos marcos seguintes.

## Fora de escopo
CRUDs, telas reais, sync, notificações, biometria, cálculo de saldo.

## Testes obrigatórios
- MoneyParser/MoneyFormatter (tabela de casos, incluindo negativos, milhar, vazio e lixo).
- Room: DAOs com banco em memória; MigrationTestHelper com baseline da v1 e schema exportado.
- Seeds idempotentes (rodar duas vezes não duplica).
- Roborazzi: tokens de cor/tipografia em claro, escuro e AMOLED.

## Critérios de aceite
- `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt` verde.
- App abre, navega entre os 4 destinos e o drawer, troca tema claro/escuro/sistema.
- Nenhum `!!`, nenhuma string hardcoded, nenhuma cor hardcoded fora de Color.kt.

**Commit sugerido:** `chore(projeto): cria fundação do app com tema, Room v1 e CI`
