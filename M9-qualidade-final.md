# M9 — Qualidade final: acessibilidade, desempenho, screenshots, CI e release v1.0

**Pré-requisito:** M1 a M8 concluídos.
**Objetivo:** fechar a v1.0 com qualidade de produto: nada de "depois a gente vê".

## Escopo
1. **Acessibilidade**: auditoria completa com TalkBack e Accessibility Scanner; `contentDescription` em ícones e gráficos; ordem de foco; valores monetários lidos por extenso ("mil duzentos e trinta e quatro reais e cinquenta e seis centavos"); alvos >= 48dp; fonte até 200% sem quebra de layout; contraste AA; nada depender só de cor; respeito a "remover animações". Corrigir tudo que encontrar e documentar o que não for possível.
2. **Desempenho**: medir com 100 mil lançamentos (gerador do M1): abertura do Painel, rolagem da lista, busca, relatórios, sync em lotes. Revisar índices, queries (EXPLAIN QUERY PLAN), chaves de LazyColumn, recomposições. Baseline Profile opcional. Documentar números antes/depois em docs/performance.md. Garantir que seeds, migrações e sync não bloqueiem a main thread e que o sync respeite o tempo de execução (6 min) e as cotas do Apps Script.
3. **Testes**: cobertura mínima por camada (domain >= 90%, repositórios >= 70%); preencher lacunas dos marcos anteriores; suíte de regressão das regras da Base §5 em um único arquivo de "casos dourados" legível; testes de migração para todas as versões do schema.
4. **Screenshots (Roborazzi)**: todas as telas em claro, escuro e fonte grande, em celular e tablet; gerar imagens para o README.
5. **CI**: ktlint, detekt, lint, testes unitários, `assembleRelease` (com keystore via secrets), falha bloqueia merge; checagem automática de strings hardcoded e de `!!`.
6. **Release**: R8 verificado (app release roda igual ao debug), ProGuard revisado, versionamento semântico (versionName/versionCode), keystore e `.env` documentados, `assembleRelease` gera APK/AAB assinado.
7. **Documentação**: README no formato do Papecon (badges, índice, funcionalidades, capturas, stack, arquitetura, estrutura de pastas, modelo de dados, passo a passo do Apps Script, build, git workflow, testes, contribuição, licença); `docs/decisions.md` completo; `docs/performance.md`; `CHANGELOG.md`; guia do usuário curto em pt-BR dentro do app (Ajuda).
8. **Checklist v1.0** (cada item com evidência: teste, print ou comando):
   - [ ] Regras da Base §5 cobertas por teste
   - [ ] Estados vazio/erro/offline em todas as telas
   - [ ] Sem `!!`, sem strings/cores hardcoded
   - [ ] Sem dado sensível em logs
   - [ ] Sync Mesclar e Substituir validados com planilha real
   - [ ] Backup JSON restaura 100%
   - [ ] TalkBack percorre os fluxos principais
   - [ ] 100 mil lançamentos sem jank perceptível
   - [ ] Release assinado instala e roda

## Fora de escopo
Funcionalidades da v1.1 e v2 (Base §9).

## Critérios de aceite
- CI 100% verde, checklist preenchido, README com capturas reais, build de release instalável.
- Lista final de pendências e bugs conhecidos em `docs/known-issues.md`, priorizada para a v1.1.

**Commit sugerido:** `chore(release): fecha a v1.0 com acessibilidade, desempenho e documentação`
