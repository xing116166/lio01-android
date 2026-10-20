package com.xboard.app

import com.xboard.app.data.Plan
import com.xboard.app.util.availablePeriods
import com.xboard.app.util.daysLeft
import com.xboard.app.util.formatMoney
import com.xboard.app.util.formatPlanTraffic
import com.xboard.app.util.formatTraffic
import com.xboard.app.util.isValidBaseUrl
import com.xboard.app.util.normalizeBaseUrl
import com.xboard.app.util.periodLabel
import com.xboard.app.util.stripHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {

    @Test
    fun trafficFormattingUsesReadableUnits() {
        assertEquals("0 MB", formatTraffic(0))
        assertEquals("512 B", formatTraffic(512))
        assertEquals("2 GB", formatTraffic(2L * 1024 * 1024 * 1024))
        assertEquals("1.5 GB", formatTraffic((1.5 * 1024 * 1024 * 1024).toLong()))
        assertEquals("1 TB", formatTraffic(1024L * 1024 * 1024 * 1024))
        assertEquals("100 GB", formatPlanTraffic(100))
        assertEquals("1 TB", formatPlanTraffic(1024))
    }

    @Test
    fun moneyFormattingTreatsInputAsCents() {
        assertEquals("¥10", formatMoney(1000))
        assertEquals("¥10.5", formatMoney(1050))
        assertEquals("¥0", formatMoney(0))
        assertEquals("$12.34", formatMoney(1234, "$"))
    }

    @Test
    fun baseUrlNormalization() {
        assertEquals("https://example.com", normalizeBaseUrl("example.com/"))
        assertEquals("https://example.com", normalizeBaseUrl("  https://example.com/  "))
        assertEquals("http://panel.local", normalizeBaseUrl("http://panel.local/api/v1"))
        assertEquals("https://example.com", normalizeBaseUrl("https://example.com/#/login"))
        assertEquals("", normalizeBaseUrl("   "))
    }

    @Test
    fun baseUrlValidation() {
        assertTrue(isValidBaseUrl("https://example.com"))
        assertTrue(isValidBaseUrl("http://192.168.1.2:8080"))
        assertTrue(isValidBaseUrl("https://localhost"))
        assertFalse(isValidBaseUrl("https://"))
        assertFalse(isValidBaseUrl(""))
    }

    @Test
    fun htmlIsStrippedForDisplay() {
        assertEquals("Hello\nWorld", stripHtml("<p>Hello<br/>World</p>"))
        assertEquals("a & b", stripHtml("a &amp; b"))
        assertEquals("", stripHtml(null))
    }

    @Test
    fun periodLabels() {
        assertEquals("月付", periodLabel("month_price"))
        assertEquals("两年付", periodLabel("two_year_price"))
    }

    @Test
    fun availablePeriodsSkipMissingPrices() {
        val plan = Plan(id = 1, name = "Pro", month_price = 1000.0, year_price = 10000.0)
        val periods = plan.availablePeriods()
        assertEquals(2, periods.size)
        assertEquals("month_price", periods[0].key)
        assertEquals("year_price", periods[1].key)
    }

    @Test
    fun expiredTimestamps() {
        assertEquals(0, daysLeft(1))
        assertEquals(-1, daysLeft(null))
        assertTrue(daysLeft(System.currentTimeMillis() / 1000 + 3 * 86400) >= 2)
    }
}
