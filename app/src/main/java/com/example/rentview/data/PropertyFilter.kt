package com.example.rentview.data

// What the tenant typed into the search box and the filter fields.
// A property is shown only if it passes every filter that has been set.
data class PropertyFilter(
    val query: String = "",
    val minRent: Double? = null,
    val maxRent: Double? = null,
    val minBedrooms: Int = 0
) {

    fun matches(property: PropertyItem): Boolean {

        // Search: the text must appear in the title or the location.
        val text = query.trim()
        if (text.isNotEmpty()) {
            val inTitle = property.title.contains(text, ignoreCase = true)
            val inLocation = property.location.contains(text, ignoreCase = true)
            if (!inTitle && !inLocation) return false
        }

        // Price range (either end can be left empty).
        val min = minRent
        if (min != null && property.rent < min) return false

        val max = maxRent
        if (max != null && property.rent > max) return false

        // Bedrooms: "at least this many".
        return property.bedrooms >= minBedrooms
    }
}
