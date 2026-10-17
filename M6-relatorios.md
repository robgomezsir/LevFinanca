# M6 — Relatórios, patrimônio líquido, previsão e insights

**Pré-requisito:** M5 concluído.
**Objetivo:** transformar os dados em entendimento: "para onde foi meu dinheiro, como estou indo, onde vou parar".

## Escopo
1. **Gráficos em Canvas do Compose** (sem biblioteca): donut, barras agrupadas, linha e área; animações suaves; cada gráfico com descrição de acessibilidade e botão "ver como tabela".
2. **Relatórios**, todos com alternador **Competência × Caixa** (Base §5.6) e filtros por período, conta e tag:
   - Despesas por categoria (donut + lista com variação vs. mês anterior; toque abre os lançamentos filtrados)
   - Receitas × despesas (barras, últimos 12 meses)
   - Fluxo de caixa (regime Caixa)
   - Tendência por categoria
   - Top favorecidos
   - Comparativo mês a mês
3. **Patrimônio líquido**: ativos (contas, investimentos com valor atual manual) − passivos (cartões, financiamentos); `NetWorthSnapshot` mensal automático (gerado no primeiro acesso do mês) e gráfico de evolução; tela de atualização manual do valor de investimentos.
4. **`ForecastEngine`** (Base §5.13): saldo projetado no fim do mês = saldo atual + pendentes + média diária de gastos variáveis dos últimos 90 dias; "pode gastar por dia"; exibir no Painel junto ao saldo previsto.
5. **`InsightEngine`** (Base §5.14, locais e por regras): categoria X% acima da média de 3 meses, maior despesa do mês, economia vs. mês anterior, orçamento a 80%/100%, fatura prevista. Painel mostra no máximo 2 insights, dispensáveis. Cada insight tem texto determinístico (sem IA externa) e link para a tela de origem.
6. **Painel completo**: com este marco o Painel passa a ter todos os blocos da Base §6.

## Fora de escopo
Exportação PDF/CSV de relatórios (M8), detecção de assinaturas (v1.1).

## Testes obrigatórios
- Agregações em SQL: Competência × Caixa dando totais diferentes em caso com compra no cartão e pagamento da fatura no mês seguinte.
- ForecastEngine: mês sem histórico, mês com pendentes, início/fim de mês.
- InsightEngine: cada regra com caso positivo e negativo.
- Snapshots de patrimônio: idempotência.
- Desempenho: relatórios com 100 mil lançamentos abrem em tempo aceitável (meta < 500 ms de consulta; documente a medição).
- Roborazzi: cada relatório e a visão em tabela.

## Critérios de aceite
- Alternar Competência/Caixa muda os números de forma explicável e documentada na própria tela (tooltip "por quê?").
- Gráficos legíveis em modo escuro e com fonte 200%.

**Commit sugerido:** `feat(relatorios): adiciona relatórios, patrimônio, previsão e insights`
