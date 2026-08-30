package com.darkmodestudio.commandcenter.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class DmsTimeFormatterTest {

    @Test
    fun testUtcIsoToLocalTimezoneConversion() {
        // UTC: 2026-08-30T14:00:00Z -> In UTC+4 (Asia/Dubai): 2026-08-30 18:00 (06:00 PM)
        val dubaiZone = ZoneId.of("Asia/Dubai")
        val formatted = DmsTimeFormatter.parseIsoToLocal("2026-08-30T14:00:00Z", dubaiZone)

        assertNotNull(formatted)
        assertEquals("Aug 30, 2026 • 06:00 PM", formatted)
    }

    @Test
    fun testUtcIsoToLocalDateOnly() {
        val dubaiZone = ZoneId.of("Asia/Dubai")
        val formattedDate = DmsTimeFormatter.parseIsoToLocalDateOnly("2026-08-30T14:00:00Z", dubaiZone)

        assertNotNull(formattedDate)
        assertEquals("Aug 30, 2026", formattedDate)
    }

    @Test
    fun testInvalidOrMissingTimestampReturnsNullWithoutFakingNow() {
        assertNull(DmsTimeFormatter.parseIsoToLocal(null))
        assertNull(DmsTimeFormatter.parseIsoToLocal(""))
        assertNull(DmsTimeFormatter.parseIsoToLocal("   "))
        assertNull(DmsTimeFormatter.parseIsoToLocal("not-a-timestamp"))
        assertNull(DmsTimeFormatter.parseIsoToLocalDateOnly("invalid"))
    }

    @Test
    fun testFormatNowReturnsCanonicalPattern() {
        val dubaiZone = ZoneId.of("Asia/Dubai")
        val now = DmsTimeFormatter.formatNow(dubaiZone)

        assertNotNull(now)
        assertTrue(now.contains("•"))
        assertTrue(now.contains("2026") || now.contains("2025") || now.contains("2024") || now.contains("2027"))
    }

    @Test
    fun testFormatEpochMillis() {
        val dubaiZone = ZoneId.of("Asia/Dubai")
        // 1788100800000L is roughly Aug 30, 2026 in epoch
        val formatted = DmsTimeFormatter.formatEpochMillis(1788100800000L, dubaiZone)
        assertNotNull(formatted)
        assertTrue(formatted.contains("•"))
    }
}
