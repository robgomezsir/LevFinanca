package com.example.lev.domain

data class SyncMergeResult<T>(
    val rows: List<T>,
    val conflicts: List<String> = emptyList(),
)

/** Conflito em janela curta entre dois lados: vai para o log visível (M7 §C). */
data class SyncConflict(
    val tab: String,
    val id: String,
    val detail: String,
)

/** Decisão do merge por aba: o que aplicar localmente e o que enviar de volta. */
data class MergeOutcome<T>(
    val applyLocally: List<T> = emptyList(),
    val pushRemote: List<T> = emptyList(),
    val conflicts: List<SyncConflict> = emptyList(),
)

object SyncMerger {
    fun <T : Any> merge(
        local: List<T>,
        remote: List<T>,
    ): List<T> = mergeWithLog(local, remote).rows

    fun <T : Any> mergeWithLog(
        local: List<T>,
        remote: List<T>,
    ): SyncMergeResult<T> {
        val byId = linkedMapOf<String, T>()
        val conflicts = linkedSetOf<String>()

        fun addRow(row: T) {
            val id = extractId(row)
            if (id.isBlank()) {
                byId["__anonymous__${byId.size}__"] = row
                return
            }

            val existing = byId[id]
            if (existing != null) {
                val sameEntity = existing::class == row::class
                val rowConflict = sameEntity && valuesDiffer(existing, row)
                if (rowConflict) {
                    conflicts += id
                }
                byId[id] = selectWinner(existing, row)
            } else {
                byId[id] = row
            }
        }

        local.forEach(::addRow)
        remote.forEach(::addRow)

        return SyncMergeResult(byId.values.toList(), conflicts.toList())
    }

    /**
     * Merge por ID com Last-Write-Wins (M7 §C): vence o maior `updatedAt`;
     * exclusões propagam por tombstone (`deleted = true` vence como qualquer
     * linha mais nova). Linhas que só existem de um lado passam direto.
     * Divergências de conteúdo com `updatedAt` a menos de [conflictWindowMs]
     * entre si vão para o log visível de conflitos, sem bloquear o LWW.
     */
    fun <T : Any> mergeRecords(
        tab: String,
        local: List<T>,
        remote: List<T>,
        idOf: (T) -> String,
        updatedAtOf: (T) -> Long,
        conflictWindowMs: Long = 120_000L,
    ): MergeOutcome<T> {
        val applyLocally = mutableListOf<T>()
        val pushRemote = mutableListOf<T>()
        val conflicts = mutableListOf<SyncConflict>()

        val remoteById = remote.associateBy(idOf)
        val seenRemote = mutableSetOf<String>()

        for (localRow in local) {
            mergeLocalRow(
                tab, localRow, remoteById, seenRemote,
                pushRemote, conflicts, idOf, updatedAtOf, conflictWindowMs,
            )?.let { applyLocally += it }
        }
        for (remoteRow in remote) {
            val id = idOf(remoteRow)
            if (id.isNotBlank() && id !in seenRemote) applyLocally += remoteRow
        }

        return MergeOutcome(applyLocally, pushRemote, conflicts)
    }

    private fun <T : Any> mergeLocalRow(
        tab: String,
        localRow: T,
        remoteById: Map<String, T>,
        seenRemote: MutableSet<String>,
        pushRemote: MutableList<T>,
        conflicts: MutableList<SyncConflict>,
        idOf: (T) -> String,
        updatedAtOf: (T) -> Long,
        conflictWindowMs: Long,
    ): T? {
        val id = idOf(localRow)
        if (id.isBlank()) return null
        val remoteRow =
            remoteById[id] ?: run {
                pushRemote += localRow
                return null
            }
        seenRemote += id
        val decision = decide(tab, id, localRow, remoteRow, updatedAtOf, conflictWindowMs) ?: return null
        decision.conflict?.let { conflicts += it }
        return when (decision.side) {
            MergeSide.LOCAL -> {
                pushRemote += localRow
                null
            }
            MergeSide.REMOTE -> remoteRow
        }
    }

    private enum class MergeSide { LOCAL, REMOTE }

    private data class MergeDecision(val side: MergeSide, val conflict: SyncConflict?)

    private fun <T : Any> decide(
        tab: String,
        id: String,
        localRow: T,
        remoteRow: T,
        updatedAtOf: (T) -> Long,
        conflictWindowMs: Long,
    ): MergeDecision? {
        if (localRow == remoteRow) return null
        val localTs = updatedAtOf(localRow)
        val remoteTs = updatedAtOf(remoteRow)
        val remoteWins = remoteTs > localTs
        val conflict =
            if (kotlin.math.abs(remoteTs - localTs) < conflictWindowMs) {
                SyncConflict(
                    tab = tab,
                    id = id,
                    detail = "Alterado nos dois lados em intervalo curto; valeu o mais recente.",
                )
            } else {
                null
            }
        return MergeDecision(
            side = if (remoteWins) MergeSide.REMOTE else MergeSide.LOCAL,
            conflict = conflict,
        )
    }

    private fun <T : Any> selectWinner(
        local: T,
        remote: T,
    ): T {
        val localDeleted = extractDeleted(local)
        val remoteDeleted = extractDeleted(remote)
        if (localDeleted != remoteDeleted) {
            return if (remoteDeleted) remote else local
        }

        val localUpdatedAt = extractUpdatedAt(local)
        val remoteUpdatedAt = extractUpdatedAt(remote)
        val localIsNewer = remoteUpdatedAt <= localUpdatedAt

        return if (localIsNewer) local else remote
    }

    private fun valuesDiffer(
        left: Any,
        right: Any,
    ): Boolean {
        val leftFields =
            left::class.java.declaredFields
                .filterNot { it.isSynthetic || it.name == "INSTANCE" }
        val rightFields =
            right::class.java.declaredFields
                .filterNot { it.isSynthetic || it.name == "INSTANCE" }

        val leftMap =
            leftFields.associate { field ->
                field.isAccessible = true
                field.name to field.get(left)
            }
        val rightMap =
            rightFields.associate { field ->
                field.isAccessible = true
                field.name to field.get(right)
            }

        return leftMap != rightMap
    }

    private fun extractId(value: Any): String {
        val id = readProperty(value, "id") ?: return ""
        return id.toString()
    }

    private fun extractUpdatedAt(value: Any): Long {
        val updatedAt = readProperty(value, "updatedAt") ?: return 0L
        return updatedAt.toString().toLongOrNull() ?: 0L
    }

    private fun extractDeleted(value: Any): Boolean {
        return readProperty(value, "deleted") as? Boolean ?: false
    }

    private fun readProperty(
        value: Any,
        propertyName: String,
    ): Any? {
        val klass = value::class
        val capitalized = propertyName.replaceFirstChar { it.uppercase() }

        val getterNames = listOf(propertyName, "get$capitalized", "is$capitalized")
        for (getterName in getterNames) {
            val method = klass.java.methods.firstOrNull { it.name == getterName && it.parameterCount == 0 }
            if (method != null) return method.invoke(value)
        }

        val field = klass.java.declaredFields.firstOrNull { it.name == propertyName }
        if (field != null) {
            field.isAccessible = true
            return field.get(value)
        }

        return null
    }
}
