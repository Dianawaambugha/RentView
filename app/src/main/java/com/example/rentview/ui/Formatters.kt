package com.example.rentview.ui

import java.util.Locale

// Small text helpers shared by the property screens.
object Formatters {

    // 45000.0 -> "KES 45,000 / month"
    fun rent(rent: Double): String =
        String.format(Locale.US, "KES %,.0f / month", rent)

    // 0 -> "Studio", 1 -> "1 bedroom", 3 -> "3 bedrooms"
    fun bedrooms(count: Int): String = when (count) {
        0 -> "Studio"
        1 -> "1 bedroom"
        else -> "$count bedrooms"
    }
}
