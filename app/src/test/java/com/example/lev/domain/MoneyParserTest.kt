package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyParserTest {
    @Test
    fun `parse empty or blank string returns 0`() {
        assertEquals(0L, MoneyParser.parse(""))
        assertEquals(0L, MoneyParser.parse("   "))
        assertEquals(0L, MoneyParser.parse(null))
    }

    @Test
    fun `parse standard BR format returns cents`() {
        assertEquals(123456L, MoneyParser.parse("R$ 1.234,56"))
        assertEquals(123456L, MoneyParser.parse("1.234,56"))
        assertEquals(123456L, MoneyParser.parse("1234,56"))
    }

    @Test
    fun `parse US format returns cents`() {
        assertEquals(123456L, MoneyParser.parse("1234.56"))
    }

    @Test
    fun `parse whole numbers assumes reais`() {
        assertEquals(123400L, MoneyParser.parse("1.234"))
        assertEquals(100L, MoneyParser.parse("1"))
        assertEquals(5000L, MoneyParser.parse("50"))
    }

    @Test
    fun `parse negative values`() {
        assertEquals(-123456L, MoneyParser.parse("-R$ 1.234,56"))
        assertEquals(-123456L, MoneyParser.parse("-1234,56"))
        assertEquals(-5000L, MoneyParser.parse("-50"))
    }

    @Test
    fun `parse garbage input returns 0 or extracts digits`() {
        assertEquals(1000L, MoneyParser.parse("abc10xyz"))
        assertEquals(0L, MoneyParser.parse("R$ -"))
    }
}
