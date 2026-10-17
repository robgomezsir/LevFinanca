/**
 * Lev — espelho da base local no Google Planilhas (M7).
 *
 * Instalação:
 *  1. Crie uma planilha no Google Drive (será a planilha do app).
 *  2. Extensões > Apps Script, apague o Code.gs de exemplo e cole este arquivo.
 *  3. Rode configurarSegredo() uma vez e informe o MESMO segredo no app
 *     (Configurações > Planilha). O segredo fica em PropertiesService,
 *     nunca no código.
 *  4. Implantar > Nova implantação > Aplicativo da Web:
 *     Executar como "Eu", acesso "Qualquer pessoa".
 *  5. Copie a URL /exec para o app e toque em "Testar conexão".
 *
 * Protocolo (POST JSON na URL /exec):
 *  campos comuns: action, requestId (UUID; reenvios idempotentes via Cache),
 *  ts (epoch ms), sig (HMAC-SHA256 de "ts.action.requestId" com o segredo),
 *  schemaVersion, deviceId.
 *  ações: ping, setup, sync_push, sync_pull, backup, compact.
 *  Respostas sempre JSON: {status, message, counts, serverTime, scriptVersion}.
 */

var SCRIPT_VERSION = '1.0.0';
var SECRET_PROPERTY = 'LEV_SECRET';

/**
 * SEGREDO DO SYNC — dois fluxos, nenhum trava a execução:
 *  A (recomendado, sem rodar nada): no editor, ícone de engrenagem
 *     (Configurações do projeto) > Propriedades do script > adicione
 *     LEV_SECRET = <seu segredo>. Pronto, sem executar função.
 *  B: Cole o segredo entre as aspas abaixo e rode configurarSegredo() uma vez.
 * Depois de salvo, o segredo fica em PropertiesService (nunca no código
 * versionado) e você pode apagar o valor daqui e salvar o projeto.
 */
var SEGREDO_DO_APP = '';

/**
 * ID DA PLANILHA — só para projeto AVULSO (criado em script.google.com).
 * Se o script foi aberto via Extensões > Apps Script DENTRO da planilha
 * (vinculado), deixe vazio. Senão: abra a planilha, copie o ID da URL
 * (https://docs.google.com/spreadsheets/d/<ID>/edit) e cole abaixo.
 */
var PLANILHA_ID = '';
var TS_WINDOW_MS = 5 * 60 * 1000;
var MAX_BATCH = 500;
var TOMBSTONE_DEFAULT_MS = 90 * 24 * 60 * 60 * 1000;

/** Abas de dados: nome, cabeçalho pt-BR e coluna de identidade (null = composta). */
var TABS = [
  { key: 'accounts', name: 'Contas', idColumn: 'ID', headers: ['ID', 'Nome', 'Tipo', 'Saldo Inicial (R$)', 'Moeda', 'Cor', 'Ícone', 'Limite (R$)', 'Dia Fechamento', 'Dia Vencimento', 'Conta Pagamento ID', 'Na Soma', 'Arquivada', 'Ordem', 'Atualizado Em', 'Excluído'] },
  { key: 'categories', name: 'Categorias', idColumn: 'ID', headers: ['ID', 'Nome', 'Tipo', 'Categoria Pai ID', 'Cor', 'Ícone', 'Arquivada', 'Ordem', 'Atualizado Em', 'Excluído'] },
  { key: 'transactions', name: 'Lancamentos', idColumn: 'ID', headers: ['ID', 'Tipo', 'Descrição', 'Valor (R$)', 'Data', 'Vencimento', 'Pago Em', 'Status', 'Conta ID', 'Conta', 'Conta Destino ID', 'Categoria ID', 'Categoria', 'Favorecido', 'Tags', 'Observações', 'Parcelamento ID', 'Parcela', 'Total Parcelas', 'Recorrência ID', 'Fatura ID', 'Meta ID', 'Conciliado', 'Criado Em', 'Atualizado Em', 'Excluído'] },
  { key: 'splits', name: 'Divisoes', idColumn: null, matchColumns: ['Lançamento ID', 'Categoria ID', 'Valor (R$)'], headers: ['Lançamento ID', 'Categoria ID', 'Valor (R$)', 'Observação', 'Atualizado Em', 'Excluído'] },
  { key: 'invoices', name: 'Faturas', idColumn: 'ID', headers: ['ID', 'Cartão ID', 'Referência (AAAA-MM)', 'Fechamento', 'Vencimento', 'Status', 'Lançamento Pagamento ID', 'Atualizado Em', 'Excluído'] },
  { key: 'budgets', name: 'Orcamentos', idColumn: 'ID', headers: ['ID', 'Mês', 'Categoria ID', 'Planejado (R$)', 'Rollover', 'Atualizado Em', 'Excluído'] },
  { key: 'goals', name: 'Metas', idColumn: 'ID', headers: ['ID', 'Nome', 'Valor Alvo (R$)', 'Prazo', 'Conta ID', 'Status', 'Cor', 'Ícone', 'Atualizado Em', 'Excluído'] },
  { key: 'goal_contributions', name: 'AportesMeta', idColumn: 'ID', headers: ['ID', 'Meta ID', 'Valor (R$)', 'Data', 'Observação', 'Lançamento ID', 'Atualizado Em', 'Excluído'] },
  { key: 'recurrences', name: 'Recorrencias', idColumn: 'ID', headers: ['ID', 'Descrição', 'Tipo', 'Valor (R$)', 'Valor Variável', 'Frequência', 'Intervalo', 'Dia', 'Início', 'Fim', 'Conta ID', 'Categoria ID', 'Favorecido', 'Observações', 'Ativa', 'Atualizado Em', 'Excluído'] },
  { key: 'rules', name: 'Regras', idColumn: 'ID', headers: ['ID', 'Campo', 'Tipo Correspondência', 'Padrão', 'Categoria ID', 'Favorecido', 'Tags', 'Prioridade', 'Ativa', 'Atualizado Em', 'Excluído'] },
  { key: 'archive', name: 'Arquivo', idColumn: 'Mês', headers: ['Mês', 'Receitas (R$)', 'Despesas (R$)', 'Saldo (R$)', 'Fechado Em', 'Observação', 'Atualizado Em', 'Excluído'] },
  { key: 'config', name: 'Config', idColumn: 'Chave', headers: ['Chave', 'Valor', 'Atualizado Em'] }
];

var REPORT_TABS = ['rel_Resumo', 'rel_Categorias', 'rel_FluxoCaixa', 'rel_Patrimonio'];

/** Colunas de dinheiro (número decimal) e de data por aba (para formatos). */
var MONEY_HINTS = ['(R$)'];
var DATE_HINTS = ['Data', 'Vencimento', 'Pago Em', 'Fechamento', 'Prazo', 'Início', 'Fim'];
var DROPDOWNS = {
  'Tipo': {
    'Lancamentos': ['Despesa', 'Receita', 'Transferência', 'Ajuste'],
    'Categorias': ['Receita', 'Despesa'],
    'Recorrencias': ['Despesa', 'Receita'],
    'Contas': ['Conta corrente', 'Poupança', 'Dinheiro', 'Cartão de crédito', 'Investimento', 'Benefício', 'Empréstimo']
  },
  'Status': {
    'Lancamentos': ['Pago', 'Pendente'],
    'Faturas': ['Aberta', 'Fechada', 'Paga'],
    'Metas': ['Ativa', 'Concluída', 'Arquivada']
  },
  'Frequência': { 'Recorrencias': ['Diária', 'Semanal', 'Mensal', 'Anual'] },
  'Campo': { 'Regras': ['Descrição', 'Favorecido'] },
  'Tipo Correspondência': { 'Regras': ['Contém', 'Começa com', 'Regex'] }
};

/* ============================ entrada ============================ */

function doGet() {
  return json({ status: 'ok', message: 'Lev sync ativo.', scriptVersion: SCRIPT_VERSION, serverTime: Date.now(), counts: {} });
}

function doPost(e) {
  try {
    var req = JSON.parse((e && e.postData && e.postData.contents) || '{}');
  } catch (err) {
    return json({ status: 'error', code: 'BAD_REQUEST', message: 'JSON inválido.', serverTime: Date.now(), scriptVersion: SCRIPT_VERSION, counts: {} });
  }
  try {
    assertAuth(req);
  } catch (err) {
    return json({ status: 'error', code: err.code || 'UNAUTHORIZED', message: String(err.message || err), serverTime: Date.now(), scriptVersion: SCRIPT_VERSION, counts: {} });
  }

  // Idempotência: reenvios com o mesmo requestId devolvem a resposta guardada.
  var cache = CacheService.getScriptCache();
  var cacheKey = 'resp_' + req.requestId;
  try {
    var cached = cache.get(cacheKey);
    if (cached) return ContentService.createTextOutput(cached).setMimeType(ContentService.MimeType.JSON);
  } catch (err) { /* sem cache: segue o fluxo */ }

  var out;
  var lock = LockService.getScriptLock();
  try {
    lock.waitLock(20000);
    out = route(req);
  } catch (err) {
    out = { status: 'error', code: err.code || 'SERVER', message: String(err.message || err), serverTime: Date.now(), scriptVersion: SCRIPT_VERSION, counts: {} };
  } finally {
    try { lock.releaseLock(); } catch (err) {}
  }
  try { cache.put(cacheKey, JSON.stringify(out), 21600); } catch (err) {}
  return json(out);
}

/** Grava o segredo em PropertiesService (rode 1x no editor). O segredo nunca fica no código. */
function configurarSegredo() {
  var secret = String(SEGREDO_DO_APP || '').trim();
  if (!secret) {
    // Sem UI não há espera infinita: tenta o diálogo só quando houver interface.
    try {
      var ui = SpreadsheetApp.getUi();
      var res = ui.prompt(
        'Segredo do sync',
        'Informe o segredo (o mesmo que será digitado no app):',
        ui.ButtonSet.OK_CANCEL
      );
      if (res.getSelectedButton() !== ui.Button.OK) {
        Logger.log('Cancelado: nada foi salvo.');
        return;
      }
      secret = String(res.getResponseText() || '').trim();
    } catch (err) {
      throw new Error(
        'Sem interface para digitar (script autônomo ou planilha fechada). ' +
        'Cole o segredo na constante SEGREDO_DO_APP no topo do Code.gs e rode de novo.'
      );
    }
  }
  if (!secret) {
    Logger.log('Segredo vazio: nada foi salvo.');
    return;
  }
  PropertiesService.getScriptProperties().setProperty(SECRET_PROPERTY, secret);
  Logger.log(
    'Segredo salvo. Apague o valor de SEGREDO_DO_APP, salve o projeto e implante ' +
    'como Aplicativo da Web (Executar como "Eu", acesso "Qualquer pessoa").'
  );
}

/** A planilha deste script: vinculada (Extensões > Apps Script) ou por ID. */
function planilhaVinculada() {
  var ativa = null;
  try {
    ativa = SpreadsheetApp.getActiveSpreadsheet();
  } catch (err) {
    ativa = null;
  }
  if (ativa) return ativa;
  var id = String(PLANILHA_ID || '').trim();
  if (id) {
    try {
      return SpreadsheetApp.openById(id);
    } catch (err) {
      throw coded(
        'SHEET_INACCESSIBLE',
        'Não abri a planilha pelo ID. Confira o PLANILHA_ID e se a planilha ' +
        'existe na mesma conta (' + String(err && err.message || err) + ').'
      );
    }
  }
  throw coded(
    'SHEET_INACCESSIBLE',
    'Script não vinculado a nenhuma planilha. Ou abra a SUA planilha no Drive > ' +
    'Extensões > Apps Script e cole o Code.gs lá, ou preencha PLANILHA_ID ' +
    'com o ID da planilha (parte da URL entre /d/ e /edit).'
  );
}

/* ============================ auth ============================ */

function assertAuth(req) {
  var secret = PropertiesService.getScriptProperties().getProperty(SECRET_PROPERTY);
  if (!secret) throw coded('SHEET_INACCESSIBLE', 'Segredo não configurado: rode configurarSegredo() no editor.');
  if (!req.action || !req.requestId || !req.ts || !req.sig) {
    throw coded('BAD_REQUEST', 'Campos obrigatórios: action, requestId, ts, sig.');
  }
  var now = Date.now();
  if (Math.abs(now - Number(req.ts)) > TS_WINDOW_MS) {
    throw coded('UNAUTHORIZED', 'Relógio dessincronizado ou requisição expirada (janela de 5 min).');
  }
  var payload = String(req.ts) + '.' + String(req.action) + '.' + String(req.requestId);
  var mac = Utilities.computeHmacSha256Signature(payload, secret);
  var expected = '';
  for (var i = 0; i < mac.length; i++) {
    var b = mac[i] < 0 ? mac[i] + 256 : mac[i];
    expected += (b < 16 ? '0' : '') + b.toString(16);
  }
  if (expected !== String(req.sig)) throw coded('INVALID_SECRET', 'Assinatura inválida: confira o segredo no app e na planilha.');
  if (Number(req.schemaVersion || 0) > 1) {
    throw coded('SCHEMA_MISMATCH', 'App mais novo que o script: atualize o Code.gs.');
  }
}

function coded(code, message) {
  var err = new Error(message);
  err.code = code;
  return err;
}

/* ============================ roteador ============================ */

function route(req) {
  var ss = planilhaVinculada();
  var now = Date.now();
  switch (String(req.action)) {
    case 'ping':
      return { status: 'ok', message: 'Pong.', counts: {}, serverTime: now, scriptVersion: SCRIPT_VERSION };
    case 'setup':
      var created = setupTabs(ss);
      return { status: 'ok', message: 'Abas prontas: ' + created + ' criadas/verificadas.', counts: { tabs: created }, serverTime: now, scriptVersion: SCRIPT_VERSION };
    case 'sync_push':
      return handlePush(ss, req, now);
    case 'sync_pull':
      return handlePull(ss, req, now);
    case 'backup':
      return handleBackup(ss, now);
    case 'compact':
      return handleCompact(ss, req, now);
    default:
      throw coded('BAD_REQUEST', 'Ação desconhecida: ' + req.action);
  }
}

function tabConfig(key) {
  for (var i = 0; i < TABS.length; i++) if (TABS[i].key === key) return TABS[i];
  throw coded('BAD_REQUEST', 'Aba desconhecida: ' + key);
}

function json(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

/* ============================ setup ============================ */

function setupTabs(ss) {
  ss.setSpreadsheetLocale('pt_BR');
  ss.setSpreadsheetTimeZone('America/Sao_Paulo');
  var count = 0;
  TABS.forEach(function (tab) {
    var sheet = ss.getSheetByName(tab.name);
    if (!sheet) {
      sheet = ss.insertSheet(tab.name);
      count++;
    }
    ensureHeader(sheet, tab);
    count++;
  });
  setupReports(ss);
  return count;
}

function ensureHeader(sheet, tab) {
  var width = tab.headers.length;
  var first = sheet.getRange(1, 1, 1, Math.max(width, sheet.getLastColumn() || width)).getValues()[0];
  var same = first.length >= width;
  if (same) {
    for (var i = 0; i < width; i++) {
      if (String(first[i] || '').trim() !== tab.headers[i]) { same = false; break; }
    }
  }
  if (!same) {
    sheet.getRange(1, 1, 1, width).setValues([tab.headers]);
  }
  // Linha 1 congelada + destaque.
  sheet.setFrozenRows(1);
  var headerRange = sheet.getRange(1, 1, 1, width);
  headerRange.setBackground('#2454B4').setFontColor('#FFFFFF').setFontWeight('bold');

  // Formatos por coluna (mapeadas pelo NOME).
  tab.headers.forEach(function (h, idx) {
    var col = idx + 1;
    if (isMoneyHeader(h)) {
      if (sheet.getLastRow() > 1) sheet.getRange(2, col, Math.max(1, sheet.getLastRow() - 1), 1).setNumberFormat('R$ #,##0.00');
    } else if (isDateHeader(h)) {
      if (sheet.getLastRow() > 1) sheet.getRange(2, col, Math.max(1, sheet.getLastRow() - 1), 1).setNumberFormat('dd/MM/yyyy');
    }
  });

  // Validações (dropdowns) sem apagar dados.
  Object.keys(DROPDOWNS).forEach(function (headerName) {
    var perTab = DROPDOWNS[headerName];
    if (!perTab[tab.name]) return;
    var colIdx = tab.headers.indexOf(headerName) + 1;
    if (colIdx < 1) return;
    var rule = SpreadsheetApp.newDataValidation()
      .requireValueInList(perTab[tab.name], true)
      .setAllowInvalid(true)
      .build();
    sheet.getRange(2, colIdx, Math.max(1, sheet.getMaxRows() - 1), 1).setDataValidation(rule);
  });
}

function isMoneyHeader(h) {
  return MONEY_HINTS.some(function (hint) { return h.indexOf(hint) >= 0; });
}

function isDateHeader(h) {
  if (h === 'Atualizado Em' || h === 'Criado Em' || h === 'Fechado Em') return false; // epoch ms
  return DATE_HINTS.some(function (hint) { return h === hint || h.indexOf(hint) === 0; });
}

function setupReports(ss) {
  ensureReportTab(ss, 'rel_Resumo', [
    ['Métrica', 'Valor'],
    ['Receitas pagas (tudo)', '=SUMIFS(Lancamentos!D:D;Lancamentos!B:B;"Receita";Lancamentos!H:H;"Pago")'],
    ['Despesas pagas (tudo)', '=SUMIFS(Lancamentos!D:D;Lancamentos!B:B;"Despesa";Lancamentos!H:H;"Pago")'],
    ['Saldo (receitas - despesas)', '=B2-B3'],
    ['Pendentes a pagar', '=SUMIFS(Lancamentos!D:D;Lancamentos!B:B;"Despesa";Lancamentos!H:H;"Pendente")'],
    ['Pendentes a receber', '=SUMIFS(Lancamentos!D:D;Lancamentos!B:B;"Receita";Lancamentos!H:H;"Pendente")']
  ]);
  ensureReportTab(ss, 'rel_Categorias', [
    ['Categoria', 'Gasto pago'],
    ['=UNIQUE(FILTER(Lancamentos!L:L;Lancamentos!B:B="Despesa";Lancamentos!L:L<>""))', '=SUMIFS(Lancamentos!D:D;Lancamentos!L:L;A2;Lancamentos!B:B;"Despesa";Lancamentos!H:H;"Pago")']
  ]);
  ensureReportTab(ss, 'rel_FluxoCaixa', [
    ['Mês', 'Receitas', 'Despesas'],
    ['=UNIQUE(ARRAYFORMULA(TEXT(Lancamentos!E2:E;"AAAA-MM")))', '=SUMIFS(Lancamentos!D:D;Lancamentos!B:B;"Receita";ARRAYFORMULA(TEXT(Lancamentos!E2:E;"AAAA-MM"));A2)', '=SUMIFS(Lancamentos!D:D;Lancamentos!B:B;"Despesa";ARRAYFORMULA(TEXT(Lancamentos!E2:E;"AAAA-MM"));A2)']
  ]);
  ensureReportTab(ss, 'rel_Patrimonio', [
    ['Conta', 'Saldo inicial'],
    ['=UNIQUE(Contas!B2:B)', '=SUMIFS(Contas!D:D;Contas!B:B;A2)']
  ]);
  // Gráficos nativos (não derrubam o setup se falharem).
  try {
    var cat = ss.getSheetByName('rel_Categorias');
    if (cat && cat.getCharts().length === 0) {
      var pie = cat.newChart().asPieChart()
        .addRange(cat.getRange('A2:B20'))
        .setPosition(1, 4, 0, 0)
        .setOption('title', 'Despesas por categoria')
        .build();
      cat.insertChart(pie);
    }
  } catch (err) {}
  try {
    var fluxo = ss.getSheetByName('rel_FluxoCaixa');
    if (fluxo && fluxo.getCharts().length === 0) {
      var col = fluxo.newChart().asColumnChart()
        .addRange(fluxo.getRange('A1:C13'))
        .setPosition(1, 5, 0, 0)
        .setOption('title', 'Receitas x despesas por mês')
        .build();
      fluxo.insertChart(col);
    }
  } catch (err) {}
}

function ensureReportTab(ss, name, seed) {
  var sheet = ss.getSheetByName(name);
  if (!sheet) {
    sheet = ss.insertSheet(name);
    sheet.getRange(1, 1, seed.length, seed[0].length).setValues(seed);
    sheet.setFrozenRows(1);
    sheet.getRange(1, 1, 1, seed[0].length).setBackground('#008FC7').setFontColor('#FFFFFF').setFontWeight('bold');
  }
  return sheet;
}

/* ============================ push ============================ */

function handlePush(ss, req, now) {
  var tab = tabConfig(req.tab);
  var rows = req.rows || [];
  if (rows.length > MAX_BATCH) throw coded('BAD_REQUEST', 'Lote acima de 500 linhas.');
  var sheet = ss.getSheetByName(tab.name);
  if (!sheet) { sheet = ss.insertSheet(tab.name); ensureHeader(sheet, tab); }
  ensureHeader(sheet, tab);

  if (req.replace) {
    // NUNCA clear() na aba inteira: só o intervalo de dados abaixo do cabeçalho.
    if (sheet.getLastRow() > 1) {
      sheet.getRange(2, 1, sheet.getLastRow() - 1, tab.headers.length).clearContent();
    }
  }
  var idx = headerIndex(sheet, tab);
  var idMap = buildIdMap(sheet, tab, idx);
  var values = [];
  rows.forEach(function (row) {
    var key = identityOf(tab, row, idx);
    var target = key != null ? lookupRow(idMap, key) : null;
    var line = tab.headers.map(function (h) { return toCell(tab, h, row[h]); });
    if (target != null && !req.replace) {
      sheet.getRange(target, 1, 1, tab.headers.length).setValues([line]);
    } else {
      values.push(line);
    }
  });
  if (values.length > 0) {
    var start = Math.max(2, sheet.getLastRow() + 1);
    sheet.getRange(start, 1, values.length, tab.headers.length).setValues(values);
    applyFormats(sheet, tab);
  }
  var total = values.length + (rows.length - values.length);
  return {
    status: 'ok',
    message: 'Enviados: ' + total + ' em ' + tab.name + '.',
    counts: { upserted: total },
    serverTime: now,
    scriptVersion: SCRIPT_VERSION
  };
}

function headerIndex(sheet, tab) {
  var header = sheet.getRange(1, 1, 1, tab.headers.length).getValues()[0];
  var idx = {};
  header.forEach(function (h, i) { idx[String(h).trim()] = i + 1; });
  tab.headers.forEach(function (h, i) { if (!idx[h]) idx[h] = i + 1; });
  return idx;
}

function buildIdMap(sheet, tab, idx) {
  var map = {};
  if (tab.idColumn == null) {
    // Divisoes: identidade composta por lançamento+categoria+valor.
    if (sheet.getLastRow() < 2) return { __match: [] };
    var vals = sheet.getRange(2, 1, sheet.getLastRow() - 1, tab.headers.length).getValues();
    var list = [];
    vals.forEach(function (r, i) {
      list.push({
        key: [cellText(r[idx['Lançamento ID'] - 1]), cellText(r[idx['Categoria ID'] - 1]), cellText(r[idx['Valor (R$)'] - 1])].join('|'),
        row: i + 2
      });
    });
    map.__match = list;
    return map;
  }
  var col = idx[tab.idColumn];
  if (!col || sheet.getLastRow() < 2) return map;
  var ids = sheet.getRange(2, col, sheet.getLastRow() - 1, 1).getValues();
  ids.forEach(function (r, i) {
    var id = cellText(r[0]);
    if (id) map[id] = i + 2;
  });
  return map;
}

function identityOf(tab, row, idx) {
  if (tab.idColumn != null) {
    var id = cellText(row[tab.idColumn]);
    return id || null;
  }
  return [cellText(row['Lançamento ID']), cellText(row['Categoria ID']), cellText(row['Valor (R$)'])].join('|');
}

/** Localiza a linha existente: por ID ou, em Divisoes, pela chave composta. */
function lookupRow(idMap, key) {
  if (idMap[key] != null) return idMap[key];
  if (idMap.__match) {
    for (var i = 0; i < idMap.__match.length; i++) {
      if (idMap.__match[i].key === key) return idMap.__match[i].row;
    }
  }
  return null;
}

/* ============================ pull ============================ */

function handlePull(ss, req, now) {
  var tab = tabConfig(req.tab);
  var sheet = ss.getSheetByName(tab.name);
  if (!sheet) throw coded('BAD_REQUEST', 'Aba inexistente: ' + tab.name);
  ensureHeader(sheet, tab);
  var since = Number(req.since || 0);
  var offset = Math.max(0, Number(req.offset || 0));
  var limit = Math.min(MAX_BATCH, Math.max(1, Number(req.limit || MAX_BATCH)));
  var idx = headerIndex(sheet, tab);
  var last = sheet.getLastRow();
  var out = [];
  var skipped = [];
  if (last >= 2) {
    var vals = sheet.getRange(2, 1, last - 1, tab.headers.length).getValues();
    for (var i = 0; i < vals.length; i++) {
      var obj = {};
      var empty = true;
      for (var c = 0; c < tab.headers.length; c++) {
        var s = serializeCell(tab.headers[c], vals[i][c]);
        if (s !== '') empty = false;
        obj[tab.headers[c]] = s;
      }
      if (empty) continue;
      var ts = epochOf(obj['Atualizado Em']);
      if (since > 0 && ts <= since) continue;
      out.push({ ts: ts, obj: obj, line: i + 2 });
    }
  }
  out.sort(function (a, b) { return a.ts - b.ts; });
  var page = out.slice(offset, offset + limit);
  page.forEach(function (r) { delete r.ts; });
  var rows = page.map(function (r) { return r.obj; });
  return {
    status: 'ok',
    message: 'Lidos: ' + rows.length + ' de ' + tab.name + '.',
    counts: { read: rows.length, skipped: skipped.length },
    rows: rows,
    skippedLines: skipped,
    hasMore: offset + limit < out.length,
    nextOffset: offset + limit,
    total: out.length,
    serverTime: now,
    scriptVersion: SCRIPT_VERSION
  };
}

/* ============================ backup & compact ============================ */

function handleBackup(ss, now) {
  var stamp = Utilities.formatDate(new Date(now), 'America/Sao_Paulo', 'yyyyMMdd_HHmm');
  var made = 0;
  TABS.forEach(function (tab) {
    var sheet = ss.getSheetByName(tab.name);
    if (!sheet) return;
    var name = '_bkp_' + stamp + '_' + tab.name;
    var old = ss.getSheetByName(name);
    if (old) ss.deleteSheet(old);
    sheet.copyTo(ss).setName(name);
    made++;
  });
  return { status: 'ok', message: 'Backup interno em abas _bkp_' + stamp + ' (' + made + ' abas).', counts: { tabs: made }, serverTime: now, scriptVersion: SCRIPT_VERSION };
}

function handleCompact(ss, req, now) {
  var olderThan = Number(req.olderThan || (now - TOMBSTONE_DEFAULT_MS));
  var removed = 0;
  TABS.forEach(function (tab) {
    var sheet = ss.getSheetByName(tab.name);
    if (!sheet || sheet.getLastRow() < 2) return;
    var idx = headerIndex(sheet, tab);
    var delCol = idx['Excluído'];
    var tsCol = idx['Atualizado Em'];
    if (!delCol || !tsCol) return;
    var vals = sheet.getRange(2, 1, sheet.getLastRow() - 1, tab.headers.length).getValues();
    for (var i = vals.length - 1; i >= 0; i--) {
      if (isTruthy(vals[i][delCol - 1]) && epochOf(cellText(vals[i][tsCol - 1])) < olderThan) {
        sheet.deleteRow(i + 2);
        removed++;
      }
    }
  });
  return { status: 'ok', message: 'Compactação: ' + removed + ' tombstones removidos.', counts: { removed: removed }, serverTime: now, scriptVersion: SCRIPT_VERSION };
}

/* ============================ onEdit (gatilho simples) ============================ */

function onEdit(e) {
  try {
    if (!e || !e.range) return;
    var sheet = e.range.getSheet();
    var tab = null;
    for (var i = 0; i < TABS.length; i++) if (TABS[i].name === sheet.getName()) tab = TABS[i];
    if (!tab) return;
    var row = e.range.getRow();
    if (row < 2) return;
    var idx = {};
    var header = sheet.getRange(1, 1, 1, tab.headers.length).getValues()[0];
    header.forEach(function (h, c) { idx[String(h).trim()] = c + 1; });
    var tsCol = idx['Atualizado Em'];
    if (tsCol) sheet.getRange(row, tsCol).setValue(Date.now());
    if (tab.idColumn) {
      var idCol = idx[tab.idColumn];
      if (idCol && !cellText(sheet.getRange(row, idCol).getValue())) {
        var first = sheet.getRange(row, 1, 1, tab.headers.length).getValues()[0];
        var hasData = first.some(function (v) { return cellText(v) !== ''; });
        if (hasData) sheet.getRange(row, idCol).setValue(Utilities.getUuid());
      }
    }
  } catch (err) { /* gatilho simples: nunca derruba a edição */ }
}

/* ============================ parsing tolerante ============================ */

function cellText(v) {
  if (v === null || v === undefined) return '';
  if (Object.prototype.toString.call(v) === '[object Date]') return Utilities.formatDate(v, 'America/Sao_Paulo', 'dd/MM/yyyy');
  if (typeof v === 'boolean') return v ? 'VERDADEIRO' : 'FALSO';
  if (typeof v === 'number') {
    if (!isFinite(v)) return '';
    return String(Math.round(v * 100) / 100);
  }
  return String(v).trim();
}

/** Serializa célula para o app: datas dd/MM/yyyy, epoch para Atualizado Em, decimais com ponto. */
function serializeCell(header, v) {
  if (v === null || v === undefined || v === '') return '';
  if (Object.prototype.toString.call(v) === '[object Date]') {
    if (header === 'Atualizado Em' || header === 'Criado Em' || header === 'Fechado Em') return String(v.getTime());
    return Utilities.formatDate(v, 'America/Sao_Paulo', 'dd/MM/yyyy');
  }
  if (typeof v === 'boolean') return v ? 'VERDADEIRO' : 'FALSO';
  if (typeof v === 'number') {
    if (!isFinite(v)) return '';
    return String(Math.round(v * 100) / 100);
  }
  return String(v).trim();
}

/** Grava célula: dinheiro BR/US → número, datas → Date, resto texto. */
function toCell(tab, header, raw) {
  var s = cellText(raw);
  if (s === '') return '';
  if (isMoneyHeader(header)) {
    var n = parseMoney(s);
    return isNaN(n) ? s : n;
  }
  if (isDateHeader(header)) {
    var d = parseDate(s);
    return d || s;
  }
  if (header === 'Atualizado Em' || header === 'Criado Em' || header === 'Fechado Em') {
    var ep = epochOf(s);
    return ep > 0 ? ep : s;
  }
  return s;
}

/** "R$ 1.234,56", "1234.56", "1234,56" → 1234.56. */
function parseMoney(s) {
  var t = String(s).replace(/R\$\s?/gi, '').replace(/\s/g, '');
  if (t.indexOf(',') >= 0) t = t.replace(/\./g, '').replace(',', '.');
  return parseFloat(t);
}

/** dd/MM/yyyy, dd/MM (ano atual) ou ISO → Date. */
function parseDate(s) {
  var t = String(s).trim();
  var m = t.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
  if (m) return new Date(Number(m[3]), Number(m[2]) - 1, Number(m[1]));
  m = t.match(/^(\d{1,2})\/(\d{1,2})$/);
  if (m) return new Date(new Date().getFullYear(), Number(m[2]) - 1, Number(m[1]));
  m = t.match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  return null;
}

function epochOf(s) {
  if (Object.prototype.toString.call(s) === '[object Date]') return s.getTime();
  var digits = String(s == null ? '' : s).replace(/\D/g, '');
  if (!digits) return 0;
  var n = Number(digits);
  return isFinite(n) ? n : 0;
}

function isTruthy(v) {
  var s = cellText(v).toUpperCase();
  return s === 'VERDADEIRO' || s === 'TRUE' || s === '1' || s === 'SIM';
}

function applyFormats(sheet, tab) {
  tab.headers.forEach(function (h, i) {
    if (sheet.getLastRow() < 2) return;
    if (isMoneyHeader(h)) sheet.getRange(2, i + 1, sheet.getLastRow() - 1, 1).setNumberFormat('R$ #,##0.00');
    else if (isDateHeader(h)) sheet.getRange(2, i + 1, sheet.getLastRow() - 1, 1).setNumberFormat('dd/MM/yyyy');
  });
}
