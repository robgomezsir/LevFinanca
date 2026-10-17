# Registros de Decisões de Arquitetura (ADR)

## 001 - Versões do Projeto (Fundação M1)
**Data:** 2026-10-09
**Contexto:** O contrato (00-BASE) exigia Kotlin 2.2.10 e AGP 9.1.1. Contudo, em virtude da incompatibilidade detectada do AGP 9.1.0/9.1.1 (que exige target 36 que levanta alertas e erros de compilação severos com as tools instaladas e também bugs incubados em testes unitários no ambiente do Runner), optamos por um downgrade estabilizado.
**Decisão:** Utilizar Android Gradle Plugin 8.5.2 e Kotlin 2.0.0 com KSP `2.0.0-1.0.21`, que são estáveis para o Java 17 e API 34.
**Consequências:** O app compilará de forma mais confiável. Será necessário avaliar atualização futura caso bibliotecas mais novas exijam Kotlin 2.2.

## 002 - Schema v1 Completo (Room)
**Data:** 2026-10-09
**Contexto:** O M1 exige que o banco de dados já possua todas as entidades listadas na Base §4 para evitar migração prematura.
**Decisão:** O AppDatabase foi exportado com versão 1 contendo `Account`, `Category`, `Transaction` e `SystemSetting` inicialmente. Modelos adicionais como `Invoice` e `Budget` podem ser acrescidos na `v1` antes de lançarmos em produção (ou mantidos para adição iterativa com `autoMigrations`, dependendo do fluxo). O soft delete (`deleted = 0`) é o padrão universal para leituras.
**Consequências:** Todas as leituras no repositório DEVEM incluir `WHERE deleted = 0`.

## 003 - Sincronização com Google Planilhas via Apps Script (M7)
**Data:** 2026-10-11
**Contexto:** A Base §1/§4 e o M7 exigem a planilha como espelho/backup/relatório, offline-first, sem servidor próprio, com merge seguro e planilha editável à mão.
**Decisão:**
- Protocolo próprio sobre POST JSON na URL `/exec`: `action/requestId/ts/sig/schemaVersion/deviceId`, HMAC-SHA256 de `ts.action.requestId`, janela de 5 min, `requestId` idempotente (dedupe no script via CacheService). OkHttp segue o 302 do Apps Script; timeout 15 s com retry exponencial (3x) em rede/5xx/HTML.
- Script `app/src/main/assets/sheets/Code.gs` (+ `appsscript.json` com `America/Sao_Paulo`, V8, escopo `spreadsheets.currentonly`): 12 abas de dados com cabeçalho pt-BR congelado, colunas mapeadas pelo NOME, formatos, validações, `LockService`, escrita em lote, `onEdit` que carimba e gera UUID, abas `rel_*` com fórmulas e gráficos, `backup` (`_bkp_*`) e `compact` (tombstones > 90 dias). Segredo em `PropertiesService` via `configurarSegredo()`.
- App: `SheetsClient` (OkHttp+Moshi com `Map<String, Any?>`, sem codegen), `SyncRepository` (Mesclar/Substituir, debounce 30 s, logs, backup JSON local), `SyncWorker` (WorkManager periódico 6 h, sem Hilt: usa o container da Application), segredo em Keystore AES-GCM (`SyncSecretStore`, nunca no Room/BuildConfig), tela Configurações > Planilha com script + passo a passo, onboarding com etapa opcional.
- Banco v3 (`MIGRATION_2_3`): `transaction_split`, `rule` e `month_closure` com colunas de sync; todos os DAOs ganharam `getAllForSync/getDirty/markSynced/upsertAll/purgeTombstones/clearAll`. Exceção documentada: a aba Divisoes não tem coluna ID no M7, então usa push integral (replace) e identidade estável por hash (lançamento+categoria+valor+nota); totais de fatura são recalculados localmente após o pull.
- Envio imediato: `notifyLocalChange()` (gancho em toda escrita do `FinanceRepository`) agenda `syncPushOnly()` com debounce de 5 s; automático ligado por padrão (`isAutoSyncEnabled`, só atua com URL+segredo); `ensurePeriodicSync()` no boot, ao salvar conexão e no toggle.
- Erros acionáveis em pt-BR (401/403, 404, script antigo, cota, HTML do Google).
**Consequências:** `INTERNET`/`ACCESS_NETWORK_STATE` no manifest; `proguard-rules.pro` criado (faltava: o release referencia o arquivo); `FinanceRepository` notifica alterações locais via callback para o debounce; chaves por aparelho (`deviceId`, `lastSyncAt`, tema etc.) nunca sobem para a aba Config. Teste manual com planilha real segue o checklist de 10 passos no README.

## 004 - Design system livro-caixa e screenshots Roborazzi
**Data:** 2026-10-13
**Contexto:** os cards usavam o mesmo raio/cor (kit SaaS genérico), o gradiente aparecia no topo e no cartão de saldo, e não havia verificação visual (claro/escuro/fonte grande).
**Decisão:** livro-caixa como identidade (linhas com LedgerLine, GradientRule como único acento, Poiret One só em títulos, 	num nos valores); LevProgressBar compartilhado com semântica; AnimatedMoneyText com contagem de 700ms desativada quando ANIMATOR_DURATION_SCALE == 0; Roborazzi 1.59.0 + Robolectric (plugin io.github.takahirom.roborazzi, ecord/verifyRoborazziDebug), activity hospedeira declarada só no manifest de debug (src/debug), referências versionadas pelo verify.
**Consequências:** src/debug/AndroidManifest.xml não vai ao release; screenshots usam amostra estática (sem AnimatedMoneyText) e assentam o relógio antes de capturar para não vazar coroutine entre testes.

Adendo 13/10: a activity hospedeira precisa estar declarada no manifest de debug, mas SEM filtro LAUNCHER (o Robolectric resolve o intent explícito só pela declaração; com LAUNCHER surgiam 2 ícones no aparelho). Referências dos screenshots versionadas em pp/src/test/screenshots via oborazzi.outputDir (o clean apagava as de uild/).
