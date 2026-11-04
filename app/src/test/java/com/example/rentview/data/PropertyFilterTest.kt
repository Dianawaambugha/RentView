package com.example.rentview.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Plain unit tests: no emulator needed. Run with Right-click > Run 'PropertyFilterTest'.
class PropertyFilterTest {

    private fun property(
        id: Long,
        title: String,
        location: String,
        rent: Double,
        bedrooms: Int
    ) = PropertyItem(
        id = id,
        title = title,
        location = location,
        rent = rent,
        bedrooms = bedrooms,
        description = "",
        isApproved = true
    )

    private val kilimani = property(1, "2BR Apartment in Kilimani", "Kilimani, Nairobi", 65000.0, 2)
    private val bedsitter = property(2, "Bedsitter near Strathmore", "Madaraka, Nairobi", 15000.0, 0)
    private val maisonette = property(3, "3BR Maisonette", "Lavington, Nairobi", 120000.0, 3)
    private val all = listOf(kilimani, bedsitter, maisonette)

    private fun ids(filter: PropertyFilter) = all.filter { filter.matches(it) }.map { it.id }

    @Test
    fun emptyFilterShowsEverything() {
        assertEquals(listOf(1L, 2L, 3L), ids(PropertyFilter()))
    }

    @Test
    fun searchMatchesTitle() {
        assertEquals(listOf(3L), ids(PropertyFilter(query = "maisonette")))
    }

    @Test
    fun searchMatchesLocation() {
        assertEquals(listOf(2L), ids(PropertyFilter(query = "madaraka")))
    }

    @Test
    fun searchIgnoresCaseAndSurroundingSpaces() {
        assertEquals(listOf(1L), ids(PropertyFilter(query = "  KILIMANI  ")))
    }

    @Test
    fun searchMatchesAcrossTitleAndLocation() {
        // "Nairobi" is only in the locations, "Strathmore" only in a title.
        assertEquals(listOf(1L, 2L, 3L), ids(PropertyFilter(query = "nairobi")))
        assertEquals(listOf(2L), ids(PropertyFilter(query = "strathmore")))
    }

    @Test
    fun searchWithNoMatchReturnsNothing() {
        assertTrue(ids(PropertyFilter(query = "mombasa")).isEmpty())
    }

    @Test
    fun blankSearchIsIgnored() {
        assertEquals(listOf(1L, 2L, 3L), ids(PropertyFilter(query = "   ")))
    }

    @Test
    fun minRentOnly() {
        assertEquals(listOf(1L, 3L), ids(PropertyFilter(minRent = 50000.0)))
    }

    @Test
    fun maxRentOnly() {
        assertEquals(listOf(1L, 2L), ids(PropertyFilter(maxRent = 65000.0)))
    }

    @Test
    fun rentBoundsAreInclusive() {
        assertTrue(PropertyFilter(minRent = 65000.0, maxRent = 65000.0).matches(kilimani))
    }

    @Test
    fun rentRange() {
        assertEquals(listOf(1L), ids(PropertyFilter(minRent = 20000.0, maxRent = 100000.0)))
    }

    @Test
    fun minRentHigherThanMaxRentShowsNothing() {
        assertTrue(ids(PropertyFilter(minRent = 100000.0, maxRent = 20000.0)).isEmpty())
    }

    @Test
    fun minBedroomsMeansAtLeast() {
        assertEquals(listOf(1L, 3L), ids(PropertyFilter(minBedrooms = 2)))
        assertEquals(listOf(3L), ids(PropertyFilter(minBedrooms = 3)))
    }

    @Test
    fun zeroMinBedroomsKeepsBedsitters() {
        assertTrue(PropertyFilter(minBedrooms = 0).matches(bedsitter))
    }

    @Test
    fun allFiltersMustPassTogether() {
        val filter = PropertyFilter(
            query = "nairobi",
            minRent = 10000.0,
            maxRent = 70000.0,
            minBedrooms = 2
        )
        assertEquals(listOf(1L), ids(filter))
    }

    @Test
    fun filtersThatMatchNothingDoNotMatchAProperty() {
        assertFalse(PropertyFilter(query = "kilimani", maxRent = 10000.0).matches(kilimani))
    }
}
