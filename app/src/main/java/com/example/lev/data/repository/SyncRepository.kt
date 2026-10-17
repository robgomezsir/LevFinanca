package com.example.lev.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.example.lev.data.local.AppDatabase
import com.example.lev.data.local.SyncSecretStore
import com.example.lev.data.model.SystemSetting
import com.example.lev.data.remote.SheetsGateway
import com.example.lev.data.work.SyncWorker
import com.example.lev.domain.InvoiceGenerator
import com.example.lev.domain.SheetRow
import com.example.lev.domain.SheetSchema
import com.example.lev.domain.SyncConflict
import com.example.lev.domain.SyncErrorKind
import com.example.lev.domain.SyncException
import com.example.lev.domain.SyncMerger
import com.example.lev.domain.SyncProtocol
import com.example.lev.domain.SyncResponse
import com.example.lev.domain.actionableMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SyncMode { MERGE, REPLACE }

enum class SyncChipState { SYNCED, PENDING, OFFLINE, ERROR, SYNCING, NEVER }

data class SyncUiState(
    val chip: SyncChipState = SyncChipState.NEVER,
    val lastSyncAt: Long? = null,
    val lastMessage: String = "Nunca sincronizado.",
    val mode: SyncMode = SyncMode.MERGE,
    val autoSync: Boolean = true,
    val hasUrl: Boolean = false,
    val hasSecret: Boolean = false,
    val pendingCount: Int = 0,
    /** Host da URL salva (diagnóstico; nunca o segredo). */
    val savedUrlHost: String = "",
)

data class SyncLogEntry(val at: Long, val text: String)

/**
 * Repositório de sincronização com Google Planilhas (M7 §C/§F).
 *
 * - Mesclar (padrão): push das linhas sujas + pull desde o último sync + merge
 *   por ID com LWW; conflitos em janela de 2 min vão para o log visível.
 * - Substituir: Enviar sobrescreve as abas; Baixar substitui o banco. Exige
 *   confirmação na UI e cria backup automático antes (JSON local + aba `_bkp`).
 * - Automático opcional: debounce de ~30 s após alterações + periódica via
 *   WorkManager ([com.example.lev.data.work.SyncWorker]).
 */
class SyncRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val secretStore: SyncSecretStore,
    private val gateway: SheetsGateway,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var debounceJob: Job? = null

    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    private val _syncLog = MutableStateFlow<List<SyncLogEntry>>(emptyList())
    val syncLog: StateFlow<List<SyncLogEntry>> = _syncLog.asStateFlow()

    private val _conflicts = MutableStateFlow<List<SyncConflict>>(emptyList())
    val conflicts: StateFlow<List<SyncConflict>> = _conflicts.asStateFlow()

    init {
        scope.launch { refreshStatus() }
    }

    // ---------- configuração ----------

    suspend fun saveConnection(
        url: String,
        secret: String,
    ) {
        val cleanUrl = url.trim().trim('\'', '"', '`', '‘', '’', '“', '”')
        require(cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
            "URL inválida: use a URL /exec da implantação."
        }
        settingsRepository.saveSetting(KEY_URL, cleanUrl)
        if (secret.isNotBlank()) secretStore.saveSecret(secret)
        ensureDeviceId()
        ensurePeriodicSync()
        refreshStatus()
    }

    suspend fun setMode(mode: SyncMode) {
        settingsRepository.saveSetting(KEY_MODE, mode.name)
        refreshStatus()
    }

    suspend fun setAutoSync(enabled: Boolean) {
        settingsRepository.saveSetting(KEY_AUTO, enabled.toString())
        ensurePeriodicSync()
        refreshStatus()
    }

    /** Chamado pelo Worker periódico: só sincroniza se o automático estiver ligado. */
    suspend fun syncIfAutoDue() {
        if (!isAutoEnabled()) return
        if (configOrNull() == null) return
        syncMerge()
    }

    /**
     * Garante a periódica em segundo plano quando há conexão + automático,
     * e cancela caso contrário. Chamada no boot, ao salvar conexão e no toggle.
     */
    suspend fun ensurePeriodicSync() {
        if (isAutoEnabled() && configOrNull() != null) {
            SyncWorker.schedule(context)
        } else {
            SyncWorker.cancel(context)
        }
    }

    suspend fun testConnection(): Result<String> =
        runCatching {
            val (url, secret, deviceId) = requireConfig()
            val res = gateway.ping(url, secret, deviceId)
            "Conexão OK (script ${res.scriptVersion.ifBlank { "?" }}): ${res.message}"
        }.mapCatching { msg ->
            addLog("Teste de conexão: $msg")
            msg
        }.recoverCatching { e ->
            // Erro de configuração (URL/segredo ausentes) mantém a própria mensagem;
            // só falha de rede/servidor vira mensagem acionável de rede.
            // Mensagem nula (ex. NetworkOnMainThreadException) leva a classe.
            val syncError = e as? SyncException
            val base = syncError?.message ?: e.message
            val msg = base ?: (SyncErrorKind.NETWORK.actionableMessage() + " [${e.javaClass.simpleName}]")
            addLog("Teste de conexão falhou: $msg")
            throw syncError ?: SyncException(SyncErrorKind.NETWORK, msg)
        }

    // ---------- entrada rápida de alterações locais ----------

    /** Chamado após cada escrita local; agenda envio imediato com debounce curto. */
    fun notifyLocalChange() {
        scope.launch {
            refreshStatus()
            if (isAutoEnabled() && _state.value.hasUrl && _state.value.hasSecret) {
                debounceJob?.cancel()
                debounceJob =
                    scope.launch {
                        delay(AUTO_DEBOUNCE_MS)
                        syncPushOnly()
                    }
            }
        }
    }

    /** Automático ligado por padrão quando há conexão configurada (desligável na UI). */
    private suspend fun isAutoEnabled(): Boolean = isAutoSyncEnabled(settingsRepository.getSetting(KEY_AUTO))

    // ---------- sync Mesclar ----------

    suspend fun syncMerge() {
        mutex.withLock {
            val (url, secret, deviceId) =
                configOrNull() ?: run {
                    setChip(SyncChipState.NEVER, "Configure a URL e o segredo da planilha.")
                    return
                }
            if (!isOnline()) {
                setChip(SyncChipState.OFFLINE, "Sem conexão. O app continua 100% offline.")
                return
            }
            setChip(SyncChipState.SYNCING, "Sincronizando…")
            try {
                ensureSetup(url, secret, deviceId)
                val tabs = buildTabs()
                val now = System.currentTimeMillis()
                var pushed = 0
                var applied = 0
                var skipped = 0
                val lastSync = settingsRepository.getSetting(KEY_LAST_AT)?.toLongOrNull() ?: 0L

                for (tab in tabs) {
                    pushed += pushDirty(url, secret, deviceId, tab, now)
                    val outcome = pullAndMerge(url, secret, deviceId, tab, lastSync, now)
                    applied += outcome.applied
                    skipped += outcome.skipped
                }
                refreshInvoiceTotals()
                settingsRepository.saveSetting(KEY_LAST_AT, now.toString())
                val msg =
                    "Enviados: $pushed • Recebidos: $applied" +
                        (if (skipped > 0) " • $skipped linhas inválidas puladas (ver log)" else "")
                setChip(SyncChipState.SYNCED, msg)
                addLog("Sync Mesclar OK. $msg")
            } catch (e: SyncException) {
                setChip(SyncChipState.ERROR, e.message ?: "")
                addLog("Sync falhou: ${e.message}")
            } catch (e: Exception) {
                val msg = "Erro inesperado: ${e.message ?: e.javaClass.simpleName}"
                setChip(SyncChipState.ERROR, msg)
                addLog("Sync falhou: $msg")
            } finally {
                refreshStatus()
            }
        }
    }

    /**
     * Envio imediato (auto-sync): sobe só as linhas sujas, sem pull/merge.
     * Rápido e barato de cota; o merge completo (pull) fica para o sync
     * manual, a periódica e a abertura com pendências remotas. Não avança
     * `lastSyncAt` para não esconder linhas remotas do próximo Mesclar.
     */
    suspend fun syncPushOnly() {
        mutex.withLock {
            val (url, secret, deviceId) = configOrNull() ?: return
            if (!isOnline()) {
                setChip(SyncChipState.OFFLINE, "Sem conexão. Alterações pendentes serão enviadas.")
                refreshStatus()
                return
            }
            setChip(SyncChipState.SYNCING, "Enviando alterações…")
            try {
                ensureSetup(url, secret, deviceId)
                val tabs = buildTabs()
                val now = System.currentTimeMillis()
                var pushed = 0
                for (tab in tabs) {
                    pushed += pushDirty(url, secret, deviceId, tab, now)
                }
                if (pushed > 0) {
                    setChip(SyncChipState.SYNCED, "Alterações enviadas.")
                    addLog("Envio imediato OK: $pushed linhas.")
                }
            } catch (e: SyncException) {
                setChip(SyncChipState.ERROR, e.message ?: "")
                addLog("Envio imediato falhou: ${e.message}")
            } catch (e: Exception) {
                val msg = "Erro inesperado: ${e.message ?: e.javaClass.simpleName}"
                setChip(SyncChipState.ERROR, msg)
                addLog("Envio imediato falhou: $msg")
            } finally {
                refreshStatus()
            }
        }
    }

    // ---------- sync Substituir (com confirmação + backup na UI) ----------

    suspend fun localCounts(): Map<String, Int> {
        val tabs = buildTabs()
        return tabs.associate { it.tab.name to it.count() }
    }

    /**
     * Enviar (Substituir): backup automático (JSON local + aba `_bkp` no
     * script) e sobrescrita das abas de dados. A UI deve confirmar com as
     * contagens de [localCounts] antes de chamar.
     */
    suspend fun pushReplace(): String {
        return mutex.withLock {
            val (url, secret, deviceId) = requireConfig()
            if (!isOnline()) throw SyncException(SyncErrorKind.OFFLINE, SyncErrorKind.OFFLINE.actionableMessage())
            setChip(SyncChipState.SYNCING, "Enviando tudo…")
            try {
                ensureSetup(url, secret, deviceId)
                val backupFile = exportLocalJson()
                val backupRes = gateway.backup(url, secret, deviceId)
                addLog("Backup no script: ${backupRes.message}")
                val tabs = buildTabs()
                val now = System.currentTimeMillis()
                var sent = 0
                for (tab in tabs) {
                    @Suppress("UNCHECKED_CAST")
                    val typed = tab as TabSync<Any>
                    val rows = typed.all().map { typed.toRow(it) }
                    for (chunk in rows.chunked(SyncProtocol.MAX_BATCH_ROWS)) {
                        gateway.push(url, secret, deviceId, typed.tab, chunk, replace = true)
                        sent += chunk.size
                    }
                    val ids = typed.allIds()
                    if (ids.isNotEmpty()) typed.markSynced(ids, now)
                }
                settingsRepository.saveSetting(KEY_LAST_AT, now.toString())
                val msg = "Substituído na planilha: $sent linhas. Backup local: ${backupFile.name}"
                setChip(SyncChipState.SYNCED, msg)
                addLog("Enviar (substituir) OK. $msg")
                msg
            } catch (e: SyncException) {
                setChip(SyncChipState.ERROR, e.message ?: "")
                addLog("Enviar falhou: ${e.message}")
                throw e
            } finally {
                refreshStatus()
            }
        }
    }

    /**
     * Baixar (Substituir): backup automático e substituição do banco local
     * pelo conteúdo da planilha. A UI deve confirmar antes de chamar.
     */
    suspend fun pullReplace(): String {
        return mutex.withLock {
            val (url, secret, deviceId) = requireConfig()
            if (!isOnline()) throw SyncException(SyncErrorKind.OFFLINE, SyncErrorKind.OFFLINE.actionableMessage())
            setChip(SyncChipState.SYNCING, "Baixando tudo…")
            try {
                ensureSetup(url, secret, deviceId)
                val backupFile = exportLocalJson()
                gateway.backup(url, secret, deviceId)
                val tabs = buildTabs()
                val now = System.currentTimeMillis()
                var received = 0
                var skipped = 0
                database.withTransaction {
                    for (tab in tabs) {
                        @Suppress("UNCHECKED_CAST")
                        val typed = tab as TabSync<Any>
                        val (values, invalid) = pullAllValues(url, secret, deviceId, typed)
                        typed.clear()
                        typed.upsert(values)
                        // O que veio do servidor já está sincronizado: carimba para não
                        // republicar tudo como "sujo" no próximo Mesclar.
                        val ids = typed.allIds()
                        if (ids.isNotEmpty()) typed.markSynced(ids, now)
                        received += values.size
                        skipped += invalid
                    }
                }
                refreshInvoiceTotals()
                settingsRepository.saveSetting(KEY_LAST_AT, now.toString())
                val msg =
                    "Banco substituído: $received linhas" +
                        (if (skipped > 0) " ($skipped inválidas puladas)" else "") +
                        ". Backup local: ${backupFile.name}"
                setChip(SyncChipState.SYNCED, msg)
                addLog("Baixar (substituir) OK. $msg")
                msg
            } catch (e: SyncException) {
                setChip(SyncChipState.ERROR, e.message ?: "")
                addLog("Baixar falhou: ${e.message}")
                throw e
            } finally {
                refreshStatus()
            }
        }
    }

    suspend fun compactTombstones(): String {
        val (url, secret, deviceId) = requireConfig()
        val olderThan = System.currentTimeMillis() - TOMBSTONE_RETENTION_MS
        val res = gateway.compact(url, secret, deviceId, olderThan)
        database.withTransaction {
            val tabs = buildTabs()
            for (tab in tabs) tab.purge(olderThan)
        }
        val msg = "Compactação OK: ${res.message}"
        addLog(msg)
        refreshStatus()
        return msg
    }

    fun scriptAssetText(): String {
        val assets = context.assets
        return assets.open(SCRIPT_ASSET).bufferedReader().use { it.readText() }
    }

    // ---------- internos ----------

    private data class PullOutcome(val applied: Int, val skipped: Int)

    private suspend fun buildTabs(): List<TabSync<*>> {
        ensureSchemaVersionRow()
        return buildSyncTabs(database)
    }

    private suspend fun pushDirty(
        url: String,
        secret: String,
        deviceId: String,
        tab: TabSync<*>,
        now: Long,
    ): Int {
        val dirty = tab.dirty()
        if (dirty.isEmpty()) return 0
        // Divisões não têm coluna ID: push integral com replace da aba no primeiro lote
        // (os seguintes são upsert idempotente por lançamento+categoria+valor).
        val replace = tab.tab.key == SheetSchema.SPLITS.key

        @Suppress("UNCHECKED_CAST")
        val typed = tab as TabSync<Any>
        val rows = if (replace) typed.all().map { typed.toRow(it) } else dirty.map { it.row }
        var sent = 0
        for (chunk in rows.chunked(SyncProtocol.MAX_BATCH_ROWS)) {
            gateway.push(url, secret, deviceId, tab.tab, chunk, replace = replace && sent == 0)
            sent += chunk.size
        }
        val ids = if (replace) typed.allIds() else dirty.map { it.id }
        if (ids.isNotEmpty()) tab.markSynced(ids, now)
        addLog("Push ${tab.tab.name}: $sent linhas.")
        return sent
    }

    private suspend fun pullAndMerge(
        url: String,
        secret: String,
        deviceId: String,
        tab: TabSync<*>,
        since: Long,
        now: Long,
    ): PullOutcome {
        val (remoteRows, skipped) = pullAllRows(url, secret, deviceId, tab, since)
        if (remoteRows.isEmpty()) return PullOutcome(0, skipped)
        @Suppress("UNCHECKED_CAST")
        val typed = tab as TabSync<Any>
        val parsed = mutableListOf<Any>()
        var invalid = skipped
        var lineNo = 1
        for (row in remoteRows) {
            lineNo++
            val result = typed.fromRow(row, lineNo)
            if (result.value != null) {
                parsed += result.value
            } else {
                invalid++
                addLog("Pulada ${tab.tab.name} ${result.error}")
            }
        }
        if (parsed.isEmpty()) return PullOutcome(0, invalid)
        val local = typed.all()
        val outcome =
            SyncMerger.mergeRecords(
                tab = tab.tab.name,
                local = local,
                remote = parsed,
                idOf = typed.idOf,
                updatedAtOf = typed.updatedAtOf,
            )
        if (outcome.applyLocally.isNotEmpty()) {
            typed.upsert(outcome.applyLocally)
        }
        if (outcome.pushRemote.isNotEmpty()) {
            val rows = outcome.pushRemote.map { typed.toRow(it) }
            for (chunk in rows.chunked(SyncProtocol.MAX_BATCH_ROWS)) {
                gateway.push(url, secret, deviceId, typed.tab, chunk, replace = false)
            }
            typed.markSynced(outcome.pushRemote.map { typed.idOf(it) }, now)
        }
        if (outcome.conflicts.isNotEmpty()) {
            _conflicts.value = (outcome.conflicts + _conflicts.value).take(MAX_CONFLICTS)
            outcome.conflicts.forEach { addLog("Conflito ${it.tab} ${it.id}: ${it.detail}") }
        }
        if (outcome.applyLocally.isNotEmpty()) {
            addLog("Pull ${tab.tab.name}: ${outcome.applyLocally.size} aplicadas.")
        }
        return PullOutcome(outcome.applyLocally.size, invalid)
    }

    private suspend fun pullAllRows(
        url: String,
        secret: String,
        deviceId: String,
        tab: TabSync<*>,
        since: Long,
    ): Pair<List<SheetRow>, Int> {
        val rows = mutableListOf<SheetRow>()
        var offset = 0
        while (true) {
            val res: SyncResponse =
                gateway.pull(url, secret, deviceId, tab.tab, since, offset, SyncProtocol.MAX_BATCH_ROWS)
            // Filtra pelas colunas conhecidas (o app grava mas ignora extras; lê só o que conhece).
            rows += res.rows.map { row -> row.filterKeys { it in tab.tab.headers } }
            if (!res.hasMore) break
            offset = res.nextOffset
            if (offset <= 0) break
        }
        return rows to 0
    }

    private suspend fun <T : Any> pullAllValues(
        url: String,
        secret: String,
        deviceId: String,
        tab: TabSync<T>,
    ): Pair<List<T>, Int> {
        val (remoteRows, _) = pullAllRows(url, secret, deviceId, tab, since = 0L)
        val values = mutableListOf<T>()
        var invalid = 0
        var lineNo = 1
        for (row in remoteRows) {
            lineNo++
            val result = tab.fromRow(row, lineNo)
            if (result.value != null) {
                values += result.value
            } else {
                invalid++
                addLog("Pulada ${tab.tab.name} ${result.error}")
            }
        }
        return values to invalid
    }

    private suspend fun refreshInvoiceTotals() {
        val cards = database.accountDao().getAllForSync().filter { it.type == "CREDIT_CARD" }
        if (cards.isEmpty()) return
        val transactions = database.transactionDao().getAllForSync()
        val existing = database.invoiceDao().getAllForSync().associateBy { it.id }
        val updates = mutableListOf<com.example.lev.data.model.Invoice>()
        for (account in cards) {
            val drafts = InvoiceGenerator.generateForAccount(account, transactions)
            for (draft in drafts) {
                val invoice = draft.toInvoice()
                val current = existing[invoice.id]
                if (current == null) {
                    updates += invoice.copy(syncedAt = null)
                } else if (current.totalCents == 0L && invoice.totalCents != 0L) {
                    updates += current.copy(totalCents = invoice.totalCents)
                }
            }
        }
        if (updates.isNotEmpty()) database.invoiceDao().upsertAll(updates)
    }

    private suspend fun exportLocalJson(): File {
        val dir = File(context.filesDir, "sync_backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "backup_$stamp.json")
        val tabs = buildTabs()
        val dump =
            tabs.associate { tab ->
                @Suppress("UNCHECKED_CAST")
                val typed = tab as TabSync<Any>
                typed.tab.key to typed.all().map { typed.toRow(it) }
            }
        file.writeText(SyncProtocol.toJson(dump))
        // Mantém os 5 backups mais recentes.
        dir.listFiles()?.sortedByDescending { it.name }?.drop(5)?.forEach { it.delete() }
        return file
    }

    private suspend fun ensureSetup(
        url: String,
        secret: String,
        deviceId: String,
    ) {
        if (settingsRepository.getSetting(KEY_SETUP_DONE) == "true") return
        val res = gateway.setup(url, secret, deviceId)
        settingsRepository.saveSetting(KEY_SETUP_DONE, "true")
        addLog("Setup da planilha: ${res.message}")
    }

    private suspend fun ensureSchemaVersionRow() {
        val dao = database.systemSettingDao()
        if (dao.getSetting(SCHEMA_VERSION_KEY) == null) {
            dao.insert(
                SystemSetting(
                    key = SCHEMA_VERSION_KEY,
                    value = DB_SCHEMA_VERSION.toString(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private suspend fun ensureDeviceId(): String {
        val existing = settingsRepository.getSetting(KEY_DEVICE_ID)
        if (!existing.isNullOrBlank()) return existing
        val fresh = UUID.randomUUID().toString()
        settingsRepository.saveSetting(KEY_DEVICE_ID, fresh)
        return fresh
    }

    private data class SyncConfig(val url: String, val secret: String, val deviceId: String)

    private suspend fun requireConfig(): SyncConfig {
        val url = settingsRepository.getSetting(KEY_URL)?.trim().orEmpty()
        val secret = secretStore.getSecret().orEmpty()
        require(url.isNotBlank() && secret.isNotBlank()) {
            "Configure a URL /exec e o segredo em Configurações > Planilha."
        }
        return SyncConfig(url, secret, ensureDeviceId())
    }

    private suspend fun configOrNull(): SyncConfig? = runCatching { requireConfig() }.getOrNull()

    private fun isOnline(): Boolean {
        val cm =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun refreshStatus() {
        val url = settingsRepository.getSetting(KEY_URL)?.trim().orEmpty()
        val hasSecret = secretStore.hasSecret()
        val mode =
            runCatching { SyncMode.valueOf(settingsRepository.getSetting(KEY_MODE) ?: "MERGE") }
                .getOrDefault(SyncMode.MERGE)
        val auto = isAutoSyncEnabled(settingsRepository.getSetting(KEY_AUTO))
        val lastAt = settingsRepository.getSetting(KEY_LAST_AT)?.toLongOrNull()
        var pending = 0
        if (url.isNotBlank() && hasSecret) {
            runCatching {
                for (tab in buildTabs()) pending += tab.dirty().size
            }
        }
        val chip =
            when {
                _state.value.chip == SyncChipState.SYNCING -> SyncChipState.SYNCING
                url.isBlank() || !hasSecret -> SyncChipState.NEVER
                !isOnline() -> SyncChipState.OFFLINE
                pending > 0 -> SyncChipState.PENDING
                lastAt != null -> SyncChipState.SYNCED
                else -> SyncChipState.NEVER
            }
        _state.value =
            SyncUiState(
                chip = chip,
                lastSyncAt = lastAt,
                lastMessage = _state.value.lastMessage,
                mode = mode,
                autoSync = auto,
                hasUrl = url.isNotBlank(),
                hasSecret = hasSecret,
                pendingCount = pending,
                savedUrlHost = runCatching { java.net.URI(url).host ?: "" }.getOrDefault(""),
            )
    }

    private fun setChip(
        chip: SyncChipState,
        message: String,
    ) {
        _state.value = _state.value.copy(chip = chip, lastMessage = message)
    }

    private fun addLog(text: String) {
        val entry = SyncLogEntry(System.currentTimeMillis(), text)
        _syncLog.value = listOf(entry).plus(_syncLog.value).take(MAX_LOG)
    }

    companion object {
        const val KEY_URL = "sync_sheets_url"
        const val KEY_MODE = "sync_mode"
        const val KEY_AUTO = "sync_auto"
        const val KEY_LAST_AT = "sync_last_at"
        const val KEY_SETUP_DONE = "sheets_setup_done"
        const val KEY_DEVICE_ID = "deviceId"
        const val KEY_LAST_MESSAGE = "sync_last_message"
        const val SCHEMA_VERSION_KEY = "schema_version"
        const val DB_SCHEMA_VERSION = 3

        /** Chaves de Config sincronizadas (nunca segredos nem estado por aparelho). */
        val SYNCED_CONFIG_KEYS = setOf(SCHEMA_VERSION_KEY)

        const val SYNC_SHEETS_URL_LEGACY = "sync_url"
        const val SCRIPT_ASSET = "sheets/Code.gs"

        private const val TOMBSTONE_RETENTION_MS = 90L * 24L * 60L * 60L * 1000L
        private const val AUTO_DEBOUNCE_MS = 5_000L
        private const val MAX_LOG = 100
        private const val MAX_CONFLICTS = 50

        // Chaves por aparelho que NUNCA sobem para a planilha.
        val LOCAL_ONLY_KEYS =
            setOf(
                KEY_DEVICE_ID, KEY_LAST_AT, KEY_SETUP_DONE, KEY_LAST_MESSAGE,
                "theme_mode", "appearance", "dynamic_color", "onboarding_complete",
                "use_biometrics", "account_reminders",
                "sync_enabled", "sync_server", "sync_token", "sync_url",
            )
    }
}

/**
 * Automático ligado por padrão (só atua com conexão configurada).
 * Apenas "false" explícito desliga.
 */
internal fun isAutoSyncEnabled(raw: String?): Boolean = raw != "false"
