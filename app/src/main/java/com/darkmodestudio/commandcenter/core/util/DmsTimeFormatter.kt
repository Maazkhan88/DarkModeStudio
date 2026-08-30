package com.darkmodestudio.commandcenter.core.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DmsTimeFormatter {

    private val canonicalFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • hh:mm a", Locale.US)
    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)

    /**
     * Parses a remote UTC ISO-8601 timestamp (e.g. "2026-08-30T14:00:00Z")
     * and converts it to the device's local timezone, formatted as "MMM dd, yyyy • hh:mm a".
     *
     * Returns null if input is null, blank, or invalid. NEVER falls back to current time.
     */
    fun parseIsoToLocal(isoString: String?, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        if (isoString.isNullOrBlank()) return null
        return try {
            val instant = Instant.parse(isoString)
            val zonedDateTime = instant.atZone(zoneId)
            canonicalFormatter.format(zonedDateTime)
        } catch (_: Exception) {
            try {
                val offsetDateTime = OffsetDateTime.parse(isoString)
                val zonedDateTime = offsetDateTime.atZoneSameInstant(zoneId)
                canonicalFormatter.format(zonedDateTime)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Formats an ISO-8601 string to date only "MMM dd, yyyy" in local timezone.
     * Returns null if missing or invalid.
     */
    fun parseIsoToLocalDateOnly(isoString: String?, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        if (isoString.isNullOrBlank()) return null
        return try {
            val instant = Instant.parse(isoString)
            val zonedDateTime = instant.atZone(zoneId)
            dateOnlyFormatter.format(zonedDateTime)
        } catch (_: Exception) {
            try {
                val offsetDateTime = OffsetDateTime.parse(isoString)
                val zonedDateTime = offsetDateTime.atZoneSameInstant(zoneId)
                dateOnlyFormatter.format(zonedDateTime)
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Returns formatted timestamp for an event created locally right now.
     */
    fun formatNow(zoneId: ZoneId = ZoneId.systemDefault()): String {
        return canonicalFormatter.format(ZonedDateTime.now(zoneId))
    }

    /**
     * Formats an epoch millisecond timestamp to local canonical string.
     */
    fun formatEpochMillis(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        if (epochMillis <= 0) return "Unknown"
        return canonicalFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zoneId))
    }
}
