package com.example.rentvew.data

import java.text.SimpleDateFormat
import java.util.Locale

// The fixed viewing slots (same ones used in the console prototype) and
// helpers for turning a stored date + slot into something we can compare.
object ViewingTime {

    val ALL_SLOTS = listOf("10:00 AM", "1:00 PM", "3:00 PM", "5:00 PM")

    private const val STORAGE_DATE_FORMAT = "yyyy-MM-dd"

    // Date and slot -> milliseconds since epoch, or null if either is invalid.
    fun toMillis(date: String, slot: String): Long? {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.US)
            parser.isLenient = false
            parser.parse("$date $slot")?.time
        } catch (e: Exception) {
            null
        }
    }

    // 2026-10-21 -> "Wed, 21 Oct 2026"
    fun displayDate(date: String): String {
        return try {
            val parser = SimpleDateFormat(STORAGE_DATE_FORMAT, Locale.US)
            parser.isLenient = false
            val parsed = parser.parse(date) ?: return date
            SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(parsed)
        } catch (e: Exception) {
            date
        }
    }

    // Year, month (0-11) and day -> 2026-10-21
    fun format(year: Int, monthZeroBased: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, day)
    }
}
