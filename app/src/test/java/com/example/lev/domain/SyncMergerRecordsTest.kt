package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergerRecordsTest {
    data class Row(val id: String, val value: String, val updatedAt: Long, val deleted: Boolean = false)

    private fun merge(
        local: List<Row>,
        remote: List<Row>,
    ) = SyncMerger.mergeRecords(
        tab = "Contas",
        local = local,
        remote = remote,
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
    )

    @Test
    fun `remoto mais novo é aplicado localmente`() {
        val outcome = merge(listOf(Row("a", "local", 1000L)), listOf(Row("a", "remote", 2000L)))
        assertEquals(listOf(Row("a", "remote", 2000L)), outcome.applyLocally)
        assertTrue(outcome.pushRemote.isEmpty())
    }

    @Test
    fun `local mais novo volta para o servidor`() {
        val outcome = merge(listOf(Row("a", "local", 3000L)), listOf(Row("a", "remote", 2000L)))
        assertEquals(listOf(Row("a", "local", 3000L)), outcome.pushRemote)
        assertTrue(outcome.applyLocally.isEmpty())
    }

    @Test
    fun `tombstone mais novo propaga a exclusão`() {
        val outcome =
            merge(
                listOf(Row("a", "local", 1000L)),
                listOf(Row("a", "remote", 5000L, deleted = true)),
            )
        assertEquals(1, outcome.applyLocally.size)
        assertTrue(outcome.applyLocally[0].deleted)
    }

    @Test
    fun `conflito em janela de 2 min vai para o log visível`() {
        val now = 10_000_000L
        val outcome =
            merge(
                listOf(Row("a", "local", now)),
                listOf(Row("a", "remote", now + 60_000L)),
            )
        assertEquals(1, outcome.conflicts.size)
        assertEquals("Contas", outcome.conflicts[0].tab)
        assertEquals("a", outcome.conflicts[0].id)
        // LWW continua valendo: o mais recente vence.
        assertEquals("remote", outcome.applyLocally[0].value)
    }

    @Test
    fun `divergência fora da janela não gera conflito`() {
        val outcome =
            merge(
                listOf(Row("a", "local", 1000L)),
                listOf(Row("a", "remote", 3_700_000L)),
            )
        assertTrue(outcome.conflicts.isEmpty())
        assertEquals("remote", outcome.applyLocally[0].value)
    }

    @Test
    fun `linhas iguais não fazem nada`() {
        val row = Row("a", "same", 1000L)
        val outcome = merge(listOf(row), listOf(row))
        assertTrue(outcome.applyLocally.isEmpty())
        assertTrue(outcome.pushRemote.isEmpty())
        assertTrue(outcome.conflicts.isEmpty())
    }

    @Test
    fun `linhas novas dos dois lados são preservadas`() {
        val outcome =
            merge(
                listOf(Row("l1", "x", 1000L)),
                listOf(Row("r1", "y", 2000L)),
            )
        assertEquals(listOf("l1"), outcome.pushRemote.map { it.id })
        assertEquals(listOf("r1"), outcome.applyLocally.map { it.id })
    }

    @Test
    fun `empate de timestamp mantém o local de forma determinística`() {
        val outcome =
            merge(
                listOf(Row("a", "local", 5000L)),
                listOf(Row("a", "remote", 5000L)),
            )
        assertEquals(1, outcome.pushRemote.size)
        assertTrue(outcome.applyLocally.isEmpty())
    }
}
