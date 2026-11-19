package com.example.rentview.data

// A rental property as displayed to tenants.
// id remains for compatibility with existing Room-based screens.
// supabaseId is the actual shared property identifier.
data class PropertyItem(
    val id: Long,
    val title: String,
    val location: String,
    val rent: Double,
    val bedrooms: Int,
    val description: String,
    val isApproved: Boolean,
    val photoUris: List<String> = emptyList(),
    val supabaseId: String? = null
)

// A viewing joined with its property's details.
// Cloud IDs are UUID strings.
data class ViewingItem(
    val id: Long,
    val propertyId: Long,
    val propertyTitle: String,
    val propertyLocation: String,
    val date: String,
    val timeSlot: String,
    val supabaseId: String? = null,
    val supabasePropertyId: String? = null
)

sealed class BookingResult {
    data class Success(
        val viewingId: Long,
        val propertyTitle: String,
        val date: String,
        val timeSlot: String,
        val supabaseViewingId: String? = null
    ) : BookingResult()

    object SlotTaken : BookingResult()
    object TenantBusy : BookingResult()
    object InPast : BookingResult()
    object PropertyUnavailable : BookingResult()

    data class Failure(val reason: String) : BookingResult()
}