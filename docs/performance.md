# Performance e qualidade do app

## Objetivos de budget
O app Lev estabelece um orçamento mínimo para garantir fluidez em operações críticas:

- `launchMs <= 1500` para abertura do painel principal
- `listRenderMs <= 220` para listagem de lançamentos e resumo
- uso de `LazyColumn` com chaves estáveis
- evitar recomposições desnecessárias nas telas de dashboard e extrato

## Como validar
1. Rodar testes de domínio de performance.
2. Medir rotas principais com instrumentação ou profile no emulador.
3. Revisar `Flow`/`StateFlow` e queries do Room para contagem de registros.
4. Verificar se a carga de dados do mês não bloqueia a main thread.

## Regras aplicadas
- valores monetários em `Long` evitam conversões frágeis e over-allocation de memória
- agregações e cálculos ficam em `domain/` e são testáveis fora da UI
- o Room deve receber filtros e projeções mínimas para manter a query leve
- sincronização em lotes deve respeitar quotas e não bloquear a interface

## Observações do projeto
O projeto já usa `compileSdk = 36` com AGP 8.5.2 e sinaliza o warning do plugin, mas a build continua funcional em ambiente local e de CI. O foco do M9 é manter a arquitetura estável e documentar o budget para futuras evoluções.
