# M4 — Recorrências e tela "A pagar / A receber"

**Pré-requisito:** M3 concluído.
**Objetivo:** automatizar lançamentos repetidos e dar ao usuário a visão de vencimentos que o Papecon dá para prazos de clientes.

## Escopo
1. **`RecurrenceEngine`** (domain): gera ocorrências PENDING até o horizonte (padrão 3 meses); idempotente (índice único `recurrenceId + date`); frequências DAILY/WEEKLY/MONTHLY/YEARLY com intervalo; dia 29/30/31 vira o último dia do mês; fim por data ou número de ocorrências; 29/fev em ano não bissexto.
2. **Valor variável** (`isVariable`): gera pendente com valor estimado (última ocorrência ou média) marcado "a confirmar"; confirmar o valor real ao pagar.
3. **Edição**: "só esta / esta e as futuras / todas"; pausar/retomar; encerrar; excluir série.
4. **Quando gera**: ao abrir o app e diariamente em segundo plano. Aqui é permitido usar WorkManager (periódico, 1x/dia) apenas para isso; registre o ADR.
5. **Cadastro** na entrada rápida ("Repetir") e tela Recorrências (lista, próxima data, valor, ativa/pausada).
6. **Tela A pagar / A receber**: lista ordenada por vencimento (equivalente à carteira ordenada por prazo do Papecon); abas A pagar / A receber / Atrasados; **linha do tempo JAN–DEZ** destacando meses com vencimentos e o mês atual; **filtro por meses** com seleção múltipla acumulativa e atalho "Limpar"; marcar como pago (com confirmação) individual e em lote, alternando Pendente ↔ Pago; data de pagamento editável.
7. **Painel**: bloco "Próximos vencimentos" (carrossel) e contador de atrasados.
8. Detecção simples de **candidatas a recorrência** (mesmo favorecido e valor ± 5% em 3 meses seguidos): apenas sugestão ao usuário, sem criar nada sozinho.

## Fora de escopo
Notificações (M8), detecção avançada de assinaturas (v1.1).

## Testes obrigatórios
- RecurrenceEngine: dia 31, fevereiro/bissexto, idempotência ao rodar duas vezes, horizonte móvel, fim por ocorrências, semanal com intervalo 2.
- Edição "esta e as futuras" não altera passadas já pagas.
- Filtro de meses acumulativo e "Limpar".
- Worker: gera sem duplicar após reinício.
- Roborazzi: A pagar com timeline, Recorrências.

## Critérios de aceite
- Criar aluguel mensal gera os próximos 3 meses; abrir o app em outro dia não duplica.
- Atrasados aparecem destacados e somam no Painel.

**Commit sugerido:** `feat(recorrencias): adiciona recorrências e tela a pagar`
