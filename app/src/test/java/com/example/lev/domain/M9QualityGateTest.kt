package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M9QualityGateTest {
    @Test
    fun `money reads in brazilian portuguese with full cents`() {
        val text = MoneyWordsFormatter.toWords(123456L)

        assertEquals("mil duzentos e trinta e quatro reais e cinquenta e seis centavos", text)
    }

    @Test
    fun `release audit flags unsafe assertions and sensitive logging`() {
        val report =
            ReleaseAudit.audit(
                listOf(
                    "val token = user!!",
                    "Log.d(\"sync\", secret)",
                    "val ok = true",
                ),
            )

        assertTrue(report.hasIssues)
        assertEquals(2, report.issues.size)
    }

    @Test
    fun `performance budget keeps launch and list operations within acceptable limits`() {
        val budget = PerformanceBudget(launchMs = 1500.0, listRenderMs = 220.0)

        assertTrue(budget.withinBudget())
    }
}
