package com.example.rentvew.data

// A rental house as shown to tenants.
data class PropertyItem(
    val id: Long,
    val title: String,
    val location: String,
    val rent: Double,
    val bedrooms: Int,
    val description: String,
    val isApproved: Boolean
)

// A viewing a tenant has booked, joined with its property's details.
data class ViewingItem(
    val id: Long,
    val propertyId: Long,
    val propertyTitle: String,
    val propertyLocation: String,
    val date: String,      // yyyy-MM-dd
    val timeSlot: String   // e.g. 10:00 AM
)

// Everything that can happen when a tenant books or reschedules a viewing.
sealed class BookingResult {
    data class Success(
        val viewingId: Long,
        val propertyTitle: String,
        val date: String,
        val timeSlot: String
    ) : BookingResult()

    object SlotTaken : BookingResult()
    object TenantBusy : BookingResult()
    object InPast : BookingResult()
    object PropertyUnavailable : BookingResult()
    data class Failure(val reason: String) : BookingResult()
}
