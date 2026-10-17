package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    @Test
    fun `format positive cents`() {
        assertEquals("R$ 1.234,56", MoneyFormatter.format(123456L))
        assertEquals("R$ 0,50", MoneyFormatter.format(50L))
        assertEquals("R$ 0,05", MoneyFormatter.format(5L))
        assertEquals("R$ 0,00", MoneyFormatter.format(0L))
    }

    @Test
    fun `format negative cents`() {
        assertEquals("-R$ 1.234,56", MoneyFormatter.format(-123456L))
        assertEquals("-R$ 0,50", MoneyFormatter.format(-50L))
    }

    @Test
    fun `format number only`() {
        assertEquals("1.234,56", MoneyFormatter.formatNumberOnly(123456L))
        assertEquals("0,50", MoneyFormatter.formatNumberOnly(-50L))
        assertEquals("0,00", MoneyFormatter.formatNumberOnly(0L))
    }
}
