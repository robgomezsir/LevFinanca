# leve

![CI](https://img.shields.io/badge/CI-GitHub_Actions-2088FF?logo=githubactions&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?logo=kotlin)
![Android](https://img.shields.io/badge/Android-API_36-3DDC84?logo=android)

App de finanças pessoais offline-first para o Brasil, pensado para registrar despesas em menos de 5 segundos, acompanhar cartões, recorrências, orçamento e visão do patrimônio sem depender de um backend próprio.

## Índice
- [Visão geral](#visão-geral)
- [Stack](#stack)
- [Arquitetura](#arquitetura)
- [Funcionalidades do produto](#funcionalidades-do-produto)
- [Modelo de dados](#modelo-de-dados)
- [Como rodar](#como-rodar)
- [Qualidade e release](#qualidade-e-release)
- [Documentação](#documentação)

## Visão geral
O leve combina domínio financeiro, regras contábeis brasileiras e um fluxo de operação rápido em Jetpack Compose. A fonte de verdade é o Room local; a planilha do Google Sheets funciona como espelho, backup e canal de sincronização opcional.

Os módulos já cobertos em domínio incluem:
- cartões e faturas com fechamento/due day
- parcelamento e lifecycle de invoice
- recorrências
- orçamento e metas
- relatórios, patrimônio líquido e insights
- sincronização segura e merge LWW
- qualidade final com auditoria de release e orçamento de performance

## Stack
- Android nativo com Kotlin 2.0.0
- Jetpack Compose + Material 3
- Room 2.7.0 com KSP
- Coroutines + Flow
- OkHttp + Moshi
- JUnit 4 + kotlinx-coroutines-test
- ktlint + detekt + Android Lint + Gradle assembleRelease

## Arquitetura
UI → ViewModels → Repositórios → DAOs/SheetsClient

Principais decisões:
- Offline-first por padrão
- Soft delete e `updatedAt`/`syncedAt` para sincronização
- Valores monetários sempre em centavos (`Long`)
- Regras contábeis em `domain/` com testes unitários
- DI manual via `AppContainer`

## Funcionalidades do produto
- dashboard com saldo total, faturas em aberto e acesso rápido ao planejamento
- contas, categorias e transações com entrada rápida (bottom sheet com teclado, conta, categoria e parcelas)
- modal de detalhes do lançamento com edição de valor/descrição/status/categoria e exclusão
- regra de cartão brasileiro para fechamento e vencimento
- parcelamento por ciclo real de faturamento
- tela de detalhes da fatura com pagamento e reabertura
- recorrências diárias, semanais, mensais e anuais com prévia e geração de lançamentos
- orçamento por categoria com rollover do mês anterior (aba Orçamento)
- metas com aportes, progresso e sugestão diária (aba Metas)
- hub de Planejamento (orçamento, metas, recorrências) acessível pelo dashboard e relatórios
- relatórios com patrimônio líquido, orçamento do mês e insights
- sincronização com planilha usando estratégia de merge seguro
- privacidade, bloqueio e importação CSV

## Modelo de dados
- `Account`: conta, cartão, investimento, benefício, empréstimo
- `Category`: categorias e subcategorias
- `Transaction`: lanço financeiro principal
- `Invoice`: fatura por mês de cartão
- `Budget`: orçamento por categoria e mês (plano + rollover)
- `Goal` + `GoalContribution`: metas financeiras e aportes
- `Recurrence`: recorrências e eventos programados
- `TransactionSplit`: divisão de um lançamento por categoria
- `Rule`: regras de categorização automática
- `MonthClosure`: resumo de um mês fechado (Arquivo)
- `SystemSetting`: configuração de usuário e sincronização

O banco Room está na versão 3 (`MIGRATION_1_2` + `MIGRATION_2_3`, sem dados perdidos na atualização).

Todos os valores monetários usam centavos em `Long` e datas usam `LocalDate` com conversores do Room.

## Sincronização com Google Planilhas (M7)
O Room é a fonte de verdade; a planilha é espelho, backup e relatório. O app funciona 100% sem internet.

- **Configurar:** Configurações > Planilha. Cole a URL `/exec` da implantação e o segredo (cifrado com Keystore AES-GCM, nunca sai do aparelho). O passo "Conectar planilha" do onboarding faz o mesmo.
- **Script:** o código está em `app/src/main/assets/sheets/Code.gs` (+ `appsscript.json`) e na própria tela de Planilha (botão Copiar + passo a passo). A ação `setup` cria as 12 abas de dados com cabeçalho pt-BR congelado, formatos e validações, além das 4 abas `rel_*` com fórmulas e gráficos.
- **Modos:** Mesclar (padrão, LWW por `updatedAt`, tombstones, log de conflitos em janela de 2 min) e Substituir (Enviar sobrescreve as abas; Baixar substitui o banco — ambos pedem confirmação com contagens e geram backup JSON local + abas `_bkp_*`).
- **Automático (sem toque manual):** ligado por padrão havendo conexão — cada novidade no app sobe em ~5 s (só push das linhas sujas, sem pull); merge completo no botão, na periódica de 6 h com rede (WorkManager) e no worker de boot. Desligável no interruptor.
- **Edição manual na planilha é suportada:** `onEdit` carimba "Atualizado Em" e gera ID; parsing tolera `R$ 1.234,56`, `1234,56`, datas `dd/MM/yyyy`, `dd/MM` e ISO; linhas inválidas são puladas e listadas no log, sem derrubar o sync.

Checklist de validação com planilha real (10 passos):
1. Crie a planilha; abra o script DENTRO dela (Extensões > Apps Script) ou use projeto avulso preenchendo `PLANILHA_ID` no código.
2. Segredo sem rodar função: no editor, engrenagem > Propriedades do script > adicione `LEV_SECRET` (alternativa: preencha `SEGREDO_DO_APP` e rode `configurarSegredo()`). Confira o `appsscript.json`. Implante como Aplicativo da Web (Eu + Qualquer pessoa, autorizando com a sua conta).
2. No app: Configurações > Planilha, salve URL + segredo e toque em Testar conexão (esperado: "Conexão OK").
3. Toque em Sincronizar agora: as 12 abas são criadas e os dados sobem.
4. Edite um valor na planilha e sincronize: o app atualiza sem perder o resto.
5. Exclua um lançamento no app e sincronize: a linha fica com Excluído = VERDADEIRO.
6. Adicione uma linha à mão na planilha (sem ID): ela ganha ID + carimbo e desce para o app.
7. Digite um valor inválido (ex.: "abc" em Valor): a linha é pulada e aparece no log com o número.
8. Teste o segredo errado: mensagem acionável, sem vazar dados.
9. Enviar tudo (Substituir): confirma com contagens, gera backup local + `_bkp_*`, sobrescreve as abas.
10. Baixar tudo (Substituir): confirma, gera backup, substitui o banco; totais de fatura recalculados.

## Como rodar
1. Clone o projeto.
2. Copie `.env.example` para `.env` e ajuste `APPS_SCRIPT_URL` se for usar sincronização com Sheets.
3. Instale o JDK 17.
4. No root do projeto, execute:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Para release:

```bash
./gradlew assembleRelease
```

## Qualidade e release
A qualidade final do app é protegida pelos guardrails de M9:
- `MoneyWordsFormatter` para leitura por extenso em PT-BR
- `ReleaseAudit` para sinalizar `!!`, logs sensíveis e parâmetros inseguros
- `PerformanceBudget` para controlar custo de render e abertura
- CI com `detekt`, `ktlintCheck`, `lintDebug`, testes e `assembleRelease`

Referências:
- [docs/performance.md](docs/performance.md)
- [CHANGELOG.md](CHANGELOG.md)
- [docs/known-issues.md](docs/known-issues.md)

## Documentação
- [00-BASE.md](00-BASE.md)
- [M3-cartoes.md](M3-cartoes.md)
- [M4-recorrencias.md](M4-recorrencias.md)
- [M5-orcamento-metas-arquivo.md](M5-orcamento-metas-arquivo.md)
- [M6-relatorios.md](M6-relatorios.md)
- [M7-sync-sheets.md](M7-sync-sheets.md)
- [M8-seguranca-notificacoes-import.md](M8-seguranca-notificacoes-import.md)
- [M9-qualidade-final.md](M9-qualidade-final.md)
- [docs/decisions.md](docs/decisions.md)
