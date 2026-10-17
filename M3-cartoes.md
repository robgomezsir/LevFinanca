# M3 — Cartões de crédito, faturas e parcelamento

**Pré-requisito:** M2 concluído.
**Objetivo:** modelar cartão com fatura, parcelamento e pagamento exatamente como o usuário brasileiro espera. É o marco de maior risco contábil: teste exaustivamente.

## Escopo
1. **Conta tipo CREDIT_CARD**: limite, `closingDay`, `dueDay`, `paymentAccountId`, parâmetro de fechamento inclusivo/exclusivo (padrão: inclusivo).
2. **`InvoiceCycle`** (domain): dado cartão e data da compra → fatura de competência (referenceMonth, closingDate, dueDate), respeitando Base §5.5: compra até o dia de fechamento entra na fatura corrente; depois, na seguinte; `dueDay < closingDay` empurra o vencimento para o mês subsequente; virada de ano; dias 29/30/31.
3. **`InstallmentSplitter`** (domain): divide em centavos exatos, resto nas primeiras parcelas (100,00 em 3x = 33,34 + 33,33 + 33,33); parcela n cai na fatura n−1 meses à frente; gera `installmentGroupId`, `installmentNumber`, `installmentTotal`.
4. **Entrada rápida**: ao escolher conta de cartão, campo "Parcelas" (1 a 48) com pré-visualização das parcelas e das faturas atingidas.
5. **Faturas**: criação/atualização automática de `Invoice` (OPEN → CLOSED na data de fechamento → PAID); tela de fatura com lançamentos, total, vencimento, contagem regressiva do fechamento e do vencimento, histórico de faturas por mês.
6. **Pagar fatura**: TRANSFER da conta pagadora para o cartão, vinculado ao `invoiceId`; pagamento parcial permitido (fatura segue CLOSED com saldo restante); fatura PAID bloqueia edição sem reabrir.
7. **Cartões**: lista com limite usado/disponível (limite − faturas em aberto), "melhor dia de compra", parcelas futuras comprometidas (soma das parcelas das próximas faturas); badge colorido.
8. **Edição/exclusão de parcelados**: "só esta parcela / esta e as seguintes / todas" (Base §5.8), recalculando divisão de centavos quando o valor total muda.
9. **Painel**: bloco "Faturas dos cartões" (fatura atual e vencimento).
10. Atualize o saldo/previsto do BalanceCalculator para tratar cartão como passivo (não entra como "dinheiro disponível").

## Fora de escopo
Notificações de vencimento (M8), recorrências em cartão (M4), importação de fatura (M8).

## Testes obrigatórios (tabela de casos no código)
- InvoiceCycle: compra no dia exato do fechamento (inclusivo e exclusivo), no dia seguinte, `dueDay < closingDay`, 31/dez → janeiro, fevereiro bissexto, closingDay 28 em mês curto.
- InstallmentSplitter: 100,00/3; 0,10/3; 1000,00/12; 1 parcela; valores grandes.
- Pagamento total e parcial; reabrir fatura; limite disponível após pagar.
- Editar "esta e as seguintes" com mudança de valor.
- Roborazzi: lista de cartões, fatura, entrada com parcelas.

## Critérios de aceite
- Cenário completo: compra 3x → três faturas corretas → pagar fatura 1 → limite e saldos corretos.
- Nenhuma divergência de centavo em nenhum teste.

**Commit sugerido:** `feat(cartoes): adiciona faturas, parcelamento e pagamento de fatura`
