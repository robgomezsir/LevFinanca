# M7 — Sincronização com Google Planilhas (Apps Script)

**Pré-requisito:** M2 concluído (idealmente M5), pois há dados reais para sincronizar.
**Objetivo:** espelhar o banco local numa planilha do usuário via Apps Script, com merge seguro, planilha editável à mão e script pronto para copiar e colar, no mesmo espírito do Papecon Office, porém sem sobrescrever às cegas.

## Escopo
### A. Abas de dados
Linha 1 = cabeçalho em pt-BR, congelada; formatos e validações aplicados pelo script. **Sempre mapear colunas pelo NOME do cabeçalho**, nunca pela posição; novas colunas só no final.
- **Contas**: ID | Nome | Tipo | Saldo Inicial (R$) | Moeda | Cor | Ícone | Limite (R$) | Dia Fechamento | Dia Vencimento | Conta Pagamento ID | Na Soma | Arquivada | Ordem | Atualizado Em | Excluído
- **Categorias**: ID | Nome | Tipo | Categoria Pai ID | Cor | Ícone | Arquivada | Ordem | Atualizado Em | Excluído
- **Lancamentos**: ID | Tipo | Descrição | Valor (R$) | Data | Vencimento | Pago Em | Status | Conta ID | Conta | Conta Destino ID | Categoria ID | Categoria | Favorecido | Tags | Observações | Parcelamento ID | Parcela | Total Parcelas | Recorrência ID | Fatura ID | Meta ID | Conciliado | Criado Em | Atualizado Em | Excluído (as colunas "Conta" e "Categoria" são só para leitura humana: o app grava mas ignora na leitura, usando os IDs)
- **Divisoes**: Lançamento ID | Categoria ID | Valor (R$) | Observação | Atualizado Em | Excluído
- **Faturas**: ID | Cartão ID | Referência (AAAA-MM) | Fechamento | Vencimento | Status | Lançamento Pagamento ID | Atualizado Em | Excluído
- **Orcamentos**: ID | Mês | Categoria ID | Planejado (R$) | Rollover | Atualizado Em | Excluído
- **Metas**: ID | Nome | Valor Alvo (R$) | Prazo | Conta ID | Status | Cor | Ícone | Atualizado Em | Excluído
- **AportesMeta**: ID | Meta ID | Valor (R$) | Data | Observação | Lançamento ID | Atualizado Em | Excluído
- **Recorrencias**: ID | Descrição | Tipo | Valor (R$) | Valor Variável | Frequência | Intervalo | Dia | Início | Fim | Conta ID | Categoria ID | Favorecido | Observações | Ativa | Atualizado Em | Excluído
- **Regras**: ID | Campo | Tipo Correspondência | Padrão | Categoria ID | Favorecido | Tags | Prioridade | Ativa | Atualizado Em | Excluído
- **Arquivo**: Mês | Receitas (R$) | Despesas (R$) | Saldo (R$) | Fechado Em | Observação | Atualizado Em | Excluído
- **Config**: Chave | Valor | Atualizado Em (inclui schema_version)

**Abas de relatório** (prefixo `rel_`: rel_Resumo, rel_Categorias, rel_FluxoCaixa, rel_Patrimonio): criadas pelo script com fórmulas (SUMIFS/QUERY) e gráficos nativos do Planilhas sobre as abas de dados. O sync NUNCA escreve nelas e NUNCA usa `clear()` na aba inteira (só no intervalo de dados abaixo do cabeçalho).

Formatos: valores em R$ como número decimal (1500.5) com formato de moeda; datas como Date real formatado dd/MM/yyyy; status/tipos em português legível (Pago/Pendente, Despesa/Receita/Transferência) com validação de dados (dropdown).

### B. Protocolo (POST JSON na URL /exec, parseado com Moshi)
- Campos comuns: `action`, `requestId` (UUID; reenvios idempotentes), `ts` (epoch ms), `sig` (HMAC-SHA256 de `ts.action.requestId` com o segredo), `schemaVersion`, `deviceId`.
- Ações: `ping` (diagnóstico e versão do script), `setup` (cria/valida abas, cabeçalhos, formatos, validações, congelamento e abas rel_), `sync_push` (upsert por ID), `sync_pull` (since, offset, limit), `backup` (cópia interna de abas `_bkp_AAAAMMDD`, sem escopos extras de Drive), `compact` (remove tombstones com mais de 90 dias).
- Lotes de até 500 linhas por requisição e por aba; o app repete lotes que falharem; upsert idempotente torna repetir seguro.
- Respostas sempre JSON: `{status, message, counts, serverTime, scriptVersion}`. Mensagem de sucesso com diagnóstico (ex.: "Enviados: 3 contas, 412 lançamentos, 18 arquivados"), exibida em toast como no Papecon.
- O Apps Script responde com redirect 302: o OkHttp deve seguir o redirect e ler o JSON do destino, sem reenviar o POST. Timeout de 15 s, retry exponencial (3 tentativas) em falhas de rede, 5xx e HTML de erro do Google.
- **Erros acionáveis** (como no Papecon): 401/403 (implantação não está como "Qualquer pessoa" ou segredo inválido), 404 (URL errada ou implantação removida), "script antigo" (scriptVersion menor que a esperada: reimplantar com Nova versão em Implantar > Gerenciar implantações e colar a nova URL /exec), cota excedida, planilha inacessível.

### C. Conflitos e modos
- **Mesclar** (padrão): merge por ID (`SyncMerger` em domain); vence o maior `updatedAt`; exclusões propagam por tombstone (`deleted = 1`); conflitos em janela de 2 minutos entre dispositivos diferentes vão para um log visível em Configurações. Primeiro envio: manda tudo; primeiro download em app vazio: importa tudo.
- **Substituir** (igual ao Papecon): Enviar sobrescreve as abas de dados; Baixar substitui o banco local. Exige confirmação com contagem de registros e cria backup automático antes (JSON local + aba `_bkp` no script).
- **Sincronização automática** opcional (ícone de auto-sync): debounce ~30 s após alterações + periódica em segundo plano (reutilize o WorkManager do M4); `SyncStatusChip` (sincronizado, pendente, offline, erro) e botão "Sincronizar agora".

### D. Edição manual na planilha (suportada)
- `onEdit` (gatilho simples) carimba "Atualizado Em" nas linhas editadas e gera UUID em linhas novas sem ID.
- Parsing tolerante no script e no app: `R$ 1.234,56`, `1234.56`, `1234,56`; datas `dd/MM/yyyy`, `dd/MM` e ISO; "pago/PAGO/Pago". Linhas inválidas são puladas e listadas no log com o número da linha; nunca derrubam o sync.

### E. O script (entregável)
- Código completo, comentado, com `formatCellValue` e `formatCellDate` do Papecon incorporados; `appsscript.json` com timeZone `America/Sao_Paulo`, runtime V8 e escopos mínimos (`spreadsheets.currentonly`); `LockService` contra escritas concorrentes; escrita em lote com `setValues` (nada de `appendRow` em laço).
- Função `configurarSegredo()` que grava o segredo em `PropertiesService` (o segredo nunca fica no código colado). Validação de HMAC e janela de 5 minutos no `ts`.
- Guardar o script como asset do app e exibi-lo na tela de Configurações com botão **Copiar** e o passo a passo (criar planilha → Extensões > Apps Script → colar → configurarSegredo → Implantar como Aplicativo da Web, Executar como "Eu", Acesso "Qualquer pessoa" → copiar URL /exec → colar no app).

### F. Lado Android
- `SheetsClient` (OkHttp + Moshi), `SyncRepository`, `SyncMerger`, tela **Configurações > Planilha**: URL, segredo, "Salvar Conexão", "Testar conexão" (ping), "Enviar", "Baixar", modo Mesclar/Substituir, status, log de sincronização e de conflitos.
- **Segredo** guardado cifrado com Android Keystore (AES-GCM) via DataStore; nunca no Room, no BuildConfig ou em texto puro. Mostrar mascarado na UI.
- Ativar no onboarding o passo "Conectar planilha" (pulável).
- Sanitização no Android (nulos, formatos BR, datas inválidas) e UI resiliente a respostas parciais.

## Fora de escopo
Compartilhamento multiusuário, criptografia ponta a ponta, notificações de sync.

## Testes obrigatórios
- `SyncMerger`: LWW, tombstones, conflito em < 2 min, linha nova dos dois lados, planilha editada à mão.
- MockWebServer: sucesso, redirect 302, 401/403/404, timeout, HTML de erro, lote parcial, reenvio idempotente (mesmo `requestId`).
- Parsing BR em todos os formatos listados.
- HMAC: assinatura válida, `ts` expirado, segredo errado.
- Roborazzi: tela de Configurações > Planilha (todos os estados do chip).
- Teste manual guiado documentado no README (checklist de 10 passos com planilha real).

## Critérios de aceite
- Enviar → editar um valor na planilha → Baixar no modo Mesclar atualiza o app sem perder o resto.
- Excluir no app → Enviar → a linha aparece com Excluído = VERDADEIRO; outro aparelho recebe a exclusão.
- Segredo errado devolve mensagem acionável, sem vazar dados.

**Commit sugerido:** `feat(sync): adiciona sincronização com Google Planilhas via Apps Script`
