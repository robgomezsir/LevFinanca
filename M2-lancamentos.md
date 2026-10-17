# M2 — Contas, categorias, lançamentos, entrada rápida, saldo e Painel básico

**Pré-requisito:** M1 concluído.
**Objetivo:** o usuário consegue configurar o app, registrar receitas, despesas e transferências em segundos e ver saldos corretos.

## Escopo
1. **Onboarding** (sem planilha ainda): boas-vindas → tema → primeira conta → revisão das categorias padrão → tour de 3 telas. Ao final, o passo "Conectar planilha" aparece desabilitado com texto "Disponível em breve" (será ligado no M7).
2. **Contas** (somente tipos não-cartão neste marco: CHECKING, SAVINGS, CASH, INVESTMENT, BENEFIT, LOAN): CRUD com badge colorido, saldo atual e previsto, arquivar/restaurar, `includeInTotals`, reordenação.
3. **Categorias**: CRUD com subcategoria (1 nível), cor, ícone, arquivar; excluir com lançamentos exige reatribuição (Base §5.11).
4. **Entrada rápida** (coração do app): FAB abre bottom sheet com teclado numérico próprio e calculadora (aceita `12,50+8`), alternador Despesa/Receita/Transferência, valor primeiro. Campos: categoria (chips das mais usadas), conta, data (Hoje/Ontem/outra), descrição com autocompletar do histórico e sugestão de categoria/conta por frequência, status Pago/Pendente, tags, observação. Ações "Salvar e novo" e "Repetir último". **Modelos/favoritos** de lançamentos frequentes.
5. **Lista de Lançamentos**: agrupada por dia com total diário; alternador Lista/Calendário; filtros (período respeitando o mês financeiro, conta, categoria, tipo, status, tags, faixa de valor); busca inteligente (descrição, categoria, valor, data, tag); seleção múltipla com ações em lote (categorizar, mudar conta, marcar como pago, excluir); swipe (pagar/excluir); toque longo compartilha o resumo. Paginação keyset.
6. **Exclusão**: soft delete com Snackbar "Desfazer" (a tela Lixeira vem no M5).
7. **Conciliação** (Base §5.9): informar saldo real → gera ADJUSTMENT → marca lançamentos como conferidos.
8. **Painel básico**: seletor de mês com swipe, cartão de saldo (atual e previsto, count-up animado), receitas × despesas do mês com barra de progresso, busca. Os demais blocos entram nos marcos seguintes.
9. **Domain**: `BalanceCalculator` (saldo atual, previsto, atrasado, transferências, ajustes) e `MonthRange` (ciclo do mês financeiro).

## Fora de escopo
Cartões/faturas/parcelas, recorrências, orçamento, anexos, importação, sync.

## Testes obrigatórios
- BalanceCalculator: saldo atual × previsto, transferência entre contas, ajuste, pendente atrasado, conta fora da soma.
- MonthRange com início do mês em 1, 15 e 28, virada de ano.
- Calculadora do teclado (expressões válidas e inválidas).
- DAOs: filtros combinados, busca, keyset pagination com muitos registros (usar o gerador do M1).
- Roborazzi: Entrada rápida, Lista, Painel, Contas, em claro/escuro/fonte grande.

## Critérios de aceite
- Registrar uma despesa leva <= 5 toques a partir do FAB.
- Saldos batem com os testes; desfazer exclusão restaura o lançamento.
- Lista fluida com 100 mil lançamentos gerados.

**Commit sugerido:** `feat(lancamentos): adiciona contas, categorias, entrada rápida e saldo`
