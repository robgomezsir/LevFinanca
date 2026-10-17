# M5 — Orçamento, metas, fechamento de mês, Arquivo e Lixeira

**Pré-requisito:** M4 concluído.
**Objetivo:** dar função ao dinheiro (orçamento e metas) e criar o ritual mensal de fechamento, com o Arquivo herdado do conceito do Papecon.

## Escopo
1. **`BudgetEngine`** (domain), modo "Limite por categoria": planejado × gasto × restante por categoria e subcategoria, por mês financeiro; rollover opcional por categoria (sobra ou estouro passa ao mês seguinte); copiar orçamento do mês anterior; total orçado × receitas previstas. O modo envelope fica para a v1.1, mas mantenha a interface do engine preparada.
2. **Tela Orçamento**: seletor de mês; lista de categorias com barra de progresso animada (verde < 80%, amarelo 80–100%, vermelho > 100%, sempre com ícone e texto, nunca só cor); editar o planejado inline; categorias sem orçamento aparecem em "Sem orçamento".
3. **Metas**: CRUD (nome, alvo, prazo opcional, conta vinculada opcional, cor, ícone); aportes (`GoalContribution`) manuais com data e observação; progresso, "quanto guardar por mês até o prazo", projeção de conclusão pelo ritmo atual; ao atingir o alvo → status ACHIEVED com comemoração discreta e envio ao Arquivo.
4. **Fechamento de mês**: checklist (lançamentos pendentes do mês, conciliação das contas, orçamento revisado) → grava `MonthClosure` (receitas, despesas, saldo, data, nota). Mês fechado fica somente leitura (bloqueia criar/editar/excluir lançamentos naquele período, com mensagem clara) até "Reabrir".
5. **Arquivo** (equivalente ao Arquivo do Papecon): cards colapsáveis com meses fechados (receitas, despesas, saldo, data de fechamento, nota), metas concluídas e contas arquivadas; ações **Restaurar**/**Reabrir**; atalho no Painel e item no drawer.
6. **Lixeira**: lista de itens com soft delete (lançamentos, contas, categorias, metas, recorrências), restaurar e excluir definitivamente; purga automática após 30 dias (verificação ao abrir o app); ao excluir definitivamente, manter tombstone para o sync (M7).
7. **Painel**: blocos "Orçamento" (3 categorias mais críticas) e "Metas".

## Fora de escopo
Envelope/zero-based, regras automáticas, notificações de orçamento (M8), sync.

## Testes obrigatórios
- BudgetEngine: rollover positivo e negativo, subcategorias somando na pai, mês com início no dia 15, categoria sem orçamento.
- Fechamento: totais corretos, bloqueio de edição, reabertura.
- Metas: projeção, atingir alvo exato e ultrapassar.
- Lixeira: purga em 30 dias, restauração mantém vínculos.
- Roborazzi: Orçamento, Metas, Arquivo, Lixeira, fechamento.

## Critérios de aceite
- Fechar julho → não é possível lançar em julho até reabrir; o Arquivo mostra o card de julho.
- Estourar 100% de uma categoria muda a barra para vermelho com ícone/texto.

**Commit sugerido:** `feat(orcamento): adiciona orçamento, metas, fechamento de mês, arquivo e lixeira`
