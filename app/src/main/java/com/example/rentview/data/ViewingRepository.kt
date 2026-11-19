package com.example.rentview.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

// All viewing operations use Supabase as the source of truth.
class ViewingRepository(
    private val properties: PropertyRepository
) {

    private val client
        get() = SupabaseClient.client

    private fun currentCloudUserId(): String? {
        return client.auth.currentSessionOrNull()?.user?.id
    }

    private fun JsonObject.text(key: String): String {
        return this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    }

    private fun JsonObject.number(key: String): Double {
        return this[key]?.jsonPrimitive?.doubleOrNull ?: 0.0
    }

    private fun JsonObject.integer(key: String): Int {
        return this[key]?.jsonPrimitive?.intOrNull ?: 0
    }

    // Get available viewing slots for a Supabase property UUID.
    suspend fun getAvailableSlots(
        propertyId: String,
        date: String,
        excludeViewingId: String? = null
    ): List<String> = withContext(Dispatchers.IO) {

        val bookedViewings = client
            .from("viewings")
            .select {
                filter {
                    eq("property_id", propertyId)
                    eq("viewing_date", date)
                    eq("status", "BOOKED")
                }
            }
            .decodeList<JsonObject>()

        val bookedSlots = bookedViewings
            .filter { it.text("id") != excludeViewingId }
            .map { it.text("viewing_slot") }
            .toSet()

        val now = System.currentTimeMillis()

        ViewingTime.ALL_SLOTS.filter { slot ->
            val slotTime = ViewingTime.toMillis(date, slot)

            slot !in bookedSlots &&
                    slotTime != null &&
                    slotTime > now
        }
    }

    // Book a viewing in Supabase.
    // The authenticated Supabase user is always the tenant.
    suspend fun book(
        propertyId: String,
        tenantId: Long,
        date: String,
        slot: String
    ): BookingResult = withContext(Dispatchers.IO) {

        val cloudTenantId = currentCloudUserId()
            ?: return@withContext BookingResult.Failure(
                "Please log in again before booking a viewing."
            )

        val slotTime = ViewingTime.toMillis(date, slot)
            ?: return@withContext BookingResult.Failure("Invalid date or time.")

        if (slotTime <= System.currentTimeMillis()) {
            return@withContext BookingResult.InPast
        }

        val property = try {
            properties.getProperty(propertyId)
        } catch (e: Exception) {
            null
        }

        if (property == null || !property.isApproved) {
            return@withContext BookingResult.PropertyUnavailable
        }

        try {
            val cloudViewingId = UUID.randomUUID().toString()

            val viewingData = buildJsonObject {
                put("id", cloudViewingId)
                put("property_id", propertyId)
                put("tenant_id", cloudTenantId)
                put("viewing_date", date)
                put("viewing_slot", slot)
                put("status", "BOOKED")
            }

            client.from("viewings").insert(viewingData)

            BookingResult.Success(
                viewingId = -1L,
                propertyTitle = property.title,
                date = date,
                timeSlot = slot,
                supabaseViewingId = cloudViewingId
            )

        } catch (e: Exception) {
            val message = e.message.orEmpty()

            when {
                message.contains("property", ignoreCase = true) &&
                        message.contains("slot", ignoreCase = true) -> {
                    BookingResult.SlotTaken
                }

                message.contains("tenant", ignoreCase = true) &&
                        message.contains("slot", ignoreCase = true) -> {
                    BookingResult.TenantBusy
                }

                message.contains("23505") -> {
                    BookingResult.SlotTaken
                }

                else -> {
                    BookingResult.Failure(
                        "Could not book this viewing. Please try again."
                    )
                }
            }
        }
    }

    // Reschedule a cloud viewing belonging to the signed-in tenant.
    suspend fun reschedule(
        viewingId: String,
        tenantId: Long,
        date: String,
        slot: String
    ): BookingResult = withContext(Dispatchers.IO) {

        val cloudTenantId = currentCloudUserId()
            ?: return@withContext BookingResult.Failure(
                "Please log in again."
            )

        val existing = getCloudViewing(viewingId, cloudTenantId)
            ?: return@withContext BookingResult.Failure(
                "This viewing no longer exists."
            )

        val slotTime = ViewingTime.toMillis(date, slot)
            ?: return@withContext BookingResult.Failure("Invalid date or time.")

        if (slotTime <= System.currentTimeMillis()) {
            return@withContext BookingResult.InPast
        }

        try {
            val updated = client.from("viewings").update(
                {
                    set("viewing_date", date)
                    set("viewing_slot", slot)
                }
            ) {
                select()
                filter {
                    eq("id", viewingId)
                    eq("tenant_id", cloudTenantId)
                    eq("status", "BOOKED")
                }
            }.decodeList<JsonObject>()

            if (updated.isEmpty()) {
                BookingResult.Failure(
                    "This viewing could not be rescheduled. It may have already changed."
                )
            } else {
                BookingResult.Success(
                    viewingId = -1L,
                    propertyTitle = existing.propertyTitle,
                    date = date,
                    timeSlot = slot,
                    supabaseViewingId = viewingId
                )
            }

        } catch (e: Exception) {
            val message = e.message.orEmpty()

            if (message.contains("23505") ||
                message.contains("slot", ignoreCase = true)
            ) {
                BookingResult.SlotTaken
            } else {
                BookingResult.Failure(
                    "Could not reschedule this viewing. Please try again."
                )
            }
        }
    }

    // Cancel a cloud viewing without deleting its history.
    suspend fun cancel(
        viewingId: String,
        tenantId: Long
    ): Boolean = withContext(Dispatchers.IO) {

        val cloudTenantId = currentCloudUserId()
            ?: return@withContext false

        try {
            val updated = client.from("viewings").update(
                {
                    set("status", "CANCELLED")
                }
            ) {
                select()
                filter {
                    eq("id", viewingId)
                    eq("tenant_id", cloudTenantId)
                    eq("status", "BOOKED")
                }
            }.decodeList<JsonObject>()

            updated.isNotEmpty()

        } catch (e: Exception) {
            false
        }
    }

    // Get all booked viewings belonging to the signed-in tenant.
    suspend fun getCloudViewings(): List<ViewingItem> =
        withContext(Dispatchers.IO) {

            val cloudTenantId = currentCloudUserId()
                ?: return@withContext emptyList()

            val records = client
                .from("viewings")
                .select {
                    filter {
                        eq("tenant_id", cloudTenantId)
                        eq("status", "BOOKED")
                    }
                }
                .decodeList<JsonObject>()

            records.mapNotNull { viewing ->

                val cloudPropertyId = viewing.text("property_id")

                val property = try {
                    properties.getProperty(cloudPropertyId)
                } catch (e: Exception) {
                    null
                } ?: return@mapNotNull null

                ViewingItem(
                    id = -1L,
                    propertyId = -1L,
                    propertyTitle = property.title,
                    propertyLocation = property.location,
                    date = viewing.text("viewing_date"),
                    timeSlot = viewing.text("viewing_slot"),
                    supabaseId = viewing.text("id"),
                    supabasePropertyId = cloudPropertyId
                )
            }.sortedBy {
                ViewingTime.toMillis(it.date, it.timeSlot) ?: Long.MAX_VALUE
            }
        }

    // Get one cloud viewing belonging to the signed-in tenant.
    suspend fun getViewing(
        viewingId: String,
        tenantId: Long
    ): ViewingItem? = withContext(Dispatchers.IO) {

        val cloudTenantId = currentCloudUserId()
            ?: return@withContext null

        getCloudViewing(viewingId, cloudTenantId)
    }

    private suspend fun getCloudViewing(
        viewingId: String,
        cloudTenantId: String
    ): ViewingItem? {

        val records = client
            .from("viewings")
            .select {
                filter {
                    eq("id", viewingId)
                    eq("tenant_id", cloudTenantId)
                    eq("status", "BOOKED")
                }
            }
            .decodeList<JsonObject>()

        val viewing = records.firstOrNull()
            ?: return null

        val cloudPropertyId = viewing.text("property_id")

        val property = properties.getProperty(cloudPropertyId)
            ?: return null

        return ViewingItem(
            id = -1L,
            propertyId = -1L,
            propertyTitle = property.title,
            propertyLocation = property.location,
            date = viewing.text("viewing_date"),
            timeSlot = viewing.text("viewing_slot"),
            supabaseId = viewing.text("id"),
            supabasePropertyId = cloudPropertyId
        )
    }
}