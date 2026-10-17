# M8 — Segurança, privacidade, notificações, atalhos, importação e exportação

**Pré-requisito:** M7 concluído (o segredo do sync já usa Keystore).
**Objetivo:** proteger os dados, lembrar o usuário na hora certa e facilitar entrada/saída de dados.

## Escopo
### A. Segurança e privacidade
- **Bloqueio do app**: BiometricPrompt com fallback para credencial do dispositivo; bloqueio automático (imediato / 1 / 5 min); `FLAG_SECURE` nas telas sensíveis; modo **ocultar valores** (toque no saldo alterna) persistente.
- Revisar `backup_rules.xml` e `data_extraction_rules.xml`: banco e DataStore fora do auto-backup do Android por padrão, com opção explícita do usuário.
- Logger próprio que vira no-op em release; auditoria do código para garantir zero dado financeiro em logs, toasts de erro e crash reports.
- Tela **Privacidade**: explica onde ficam os dados, o que vai para a planilha, permite "apagar tudo" (local, com confirmação dupla) e "desvincular planilha".
- Prévia de notificação sem valores na tela de bloqueio (configurável).
- Anexo de **foto do comprovante** (somente local, em armazenamento privado do app, miniatura no lançamento).

### B. Notificações e atalhos
- Canais separados (vencimentos, orçamento, resumo, sync); permissão `POST_NOTIFICATIONS` (Android 13+) pedida no contexto certo, com explicação.
- Lembretes: contas a vencer (X dias antes e no dia), fatura fechando/vencendo, orçamento a 80% e 100%, recorrência variável aguardando valor, resumo semanal. Agendamento com WorkManager periódico; alarme exato só se estritamente necessário e permitido (registre ADR).
- Configuração por tipo de lembrete (liga/desliga, antecedência, horário).
- **Atalhos dinâmicos** do launcher ("Nova despesa", "Nova receita") e **Share target** (texto compartilhado vira rascunho de lançamento).

### C. Importação e exportação
- **Importar CSV** (OFX fica para a v1.1): tela de mapeamento de colunas, perfis salvos editáveis para bancos brasileiros comuns, pré-visualização, escolha da conta de destino, detecção de duplicatas (data + valor + descrição normalizada), `importBatchId` para **Desfazer importação**. Tratar formatos BR (vírgula decimal, dd/MM/yyyy, sinal de despesa negativo ou coluna D/C).
- **Exportar**: CSV por período/filtro, backup completo em JSON versionado (e restauração a partir dele, com validação de schema), relatório mensal em PDF (`PdfDocument`), compartilhamento via Intent.
- Aviso nas Configurações recomendando exportação periódica: a planilha não substitui o backup local.

## Fora de escopo
OFX, Regras automáticas, widget, criptografia ponta a ponta (roadmap).

## Testes obrigatórios
- Importação: arquivo com 2 mil linhas, duplicatas, formatos BR, linhas inválidas (relatório de erros), desfazer lote.
- Backup JSON: exportar → apagar tudo → restaurar → comparar igualdade dos dados.
- Bloqueio: trava ao ir para segundo plano conforme a configuração; ocultar valores persiste.
- Lembretes: cálculo de datas de disparo (fechamento, vencimento, antecedência).
- Auditoria automatizada (teste/detekt custom ou grep no CI) proibindo `Log.` direto fora do logger próprio.
- Roborazzi: Privacidade, mapeamento de importação, preferências de notificação.

## Critérios de aceite
- Com o bloqueio ativo, o app não mostra conteúdo no seletor de apps recentes nas telas sensíveis.
- Importar um CSV bancário real e desfazer o lote deixa o banco idêntico ao anterior.

**Commit sugerido:** `feat(seguranca): adiciona bloqueio, notificações, importação e exportação`
