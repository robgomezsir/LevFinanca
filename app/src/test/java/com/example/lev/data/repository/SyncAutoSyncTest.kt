package com.example.lev.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncAutoSyncTest {
    @Test
    fun `automático ligado por padrão`() {
        assertTrue(isAutoSyncEnabled(null))
        assertTrue(isAutoSyncEnabled("true"))
        assertTrue(isAutoSyncEnabled(""))
    }

    @Test
    fun `apenas false explícito desliga`() {
        assertFalse(isAutoSyncEnabled("false"))
    }
}
