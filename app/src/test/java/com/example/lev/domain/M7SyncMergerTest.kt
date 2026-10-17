package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M7SyncMergerTest {
    data class Row(
        val id: String,
        val value: String,
        val updatedAt: Long,
        val deleted: Boolean = false,
    )

    @Test
    fun `merger uses last write wins for same row`() {
        val local = Row("acc-1", "local", 1000L)
        val remote = Row("acc-1", "remote", 2000L)

        val merged = SyncMerger.merge(listOf(local), listOf(remote))

        assertEquals("remote", merged[0].value)
    }

    @Test
    fun `tombstones win over stale data`() {
        val local = Row("acc-2", "stale", 1000L)
        val remote = Row("acc-2", "fresh", 5000L, deleted = true)

        val merged = SyncMerger.merge(listOf(local), listOf(remote))

        assertTrue(merged[0].deleted)
    }

    @Test
    fun `conflicts within two minutes are kept in a visible log`() {
        val local = Row("acc-3", "local", 1000L)
        val remote = Row("acc-3", "remote", 1500000L)

        val merged = SyncMerger.mergeWithLog(listOf(local), listOf(remote))

        assertEquals("remote", merged.rows[0].value)
        assertEquals(1, merged.conflicts.size)
    }

    @Test
    fun `new rows on both sides are preserved`() {
        val local = listOf(Row("local-1", "from-local", 1000L))
        val remote = listOf(Row("remote-1", "from-remote", 2000L))

        val merged = SyncMerger.merge(listOf(local), listOf(remote))

        assertEquals(2, merged.size)
    }
}
