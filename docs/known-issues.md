# Known issues

## Encontrado em teste no aparelho (2026-10-11)

- ~~**Recorrência pausada fica inalcançável.**~~ **Corrigido** em 2026-10-11: a lista agora tem a
  seção "Pausadas" com as mesmas ações de retomar e excluir, e o estado vazio não aparece mais
  quando só existem pausadas.
- **O rótulo do switch diz "Ativa" mesmo com a recorrência pausada.** O texto do card é fixo e só
  o estado do switch indica a pausa; o rótulo deveria acompanhar (`Ativa` / `Pausada`).
- **"Excluir" não pede confirmação.** Em
  [RecurrencesScreen.kt](app/src/main/java/com/example/lev/ui/screens/recurring/RecurrencesScreen.kt),
  o botão chama `onDelete` direto, diferente do modal de lançamento, que confirma antes. Um toque
  acidental apaga a série — foi assim que a recorrência de teste foi removida no aparelho.
- Sem `sqlite3` em `/system/bin` no aparelho de teste: inspeção/ajuste manual do banco exige
  copiar o arquivo para fora (lembrar de levar `-wal` e `-shm` juntos, senão a cópia fica
  corrompida).

## Lista de pendências para a v1.1
- revisar fluxo de edição/exclusão de parcelas no app real
- completar sincronização de planilha com Apps Script e revisão de permissões
- ampliar testes de migração de schema para versões futuras (MIGRATION_1_2 criado, falta MigrationTestHelper)
- validar acessibilidade em telas de edição e modais com TalkBack
- considerar seleção de data via DatePicker nativo nos modais (hoje entrada manual AAAA-MM-DD)
- planejar geração automática de recorrências vencidas via WorkManager

## Status atual
Os módulos de domínio principais já estão estáveis e validados por testes. O que ainda precisa de refinamento é a integração de UI e o fechamento operacional da release final, e não o motor financeiro base em si.
