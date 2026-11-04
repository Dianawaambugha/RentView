package com.example.rentview.data

import android.database.Cursor
import com.example.rentview.UserDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Repository for viewings. All the booking rules live here.
class ViewingRepository(
    private val database: UserDatabaseHelper,
    private val properties: PropertyRepository
) {

    // Get available slots for a property on a date.
// Exclude a viewing when rescheduling.
    suspend fun getAvailableSlots(
        propertyId: Long,
        date: String,
        excludeViewingId: Long = 0L
    ): List<String> = withContext(Dispatchers.IO) {

        val booked = mutableSetOf<String>()

        database.getBookedSlots(propertyId, date, excludeViewingId).use { cursor ->
            while (cursor.moveToNext()) {
                booked.add(cursor.getString(0))
            }
        }

        val now = System.currentTimeMillis()

        ViewingTime.ALL_SLOTS.filter { slot ->
            val slotTime = ViewingTime.toMillis(date, slot)
            slot !in booked && slotTime != null && slotTime > now
        }
    }

    // CREATE: Book a property viewing.
    suspend fun book(
        propertyId: Long,
        tenantId: Long,
        date: String,
        slot: String
    ): BookingResult = withContext(Dispatchers.IO) {

        val slotTime = ViewingTime.toMillis(date, slot)
            ?: return@withContext BookingResult.Failure("Invalid date or time.")

        if (slotTime <= System.currentTimeMillis()) {
            return@withContext BookingResult.InPast
        }

        val property = properties.getProperty(propertyId)

        if (property == null || !property.isApproved) {
            return@withContext BookingResult.PropertyUnavailable
        }

        if (isSlotBooked(propertyId, date, slot, 0L)) {
            return@withContext BookingResult.SlotTaken
        }

        if (database.tenantHasViewingAt(tenantId, date, slot, 0L)) {
            return@withContext BookingResult.TenantBusy
        }

        val viewingId = database.insertViewing(
            propertyId,
            tenantId,
            date,
            slot
        )

        if (viewingId == -1L) {
            BookingResult.SlotTaken
        } else {
            BookingResult.Success(
                viewingId,
                property.title,
                date,
                slot
            )
        }
    }

    // UPDATE: Reschedule a viewing.
    suspend fun reschedule(
        viewingId: Long,
        tenantId: Long,
        date: String,
        slot: String
    ): BookingResult = withContext(Dispatchers.IO) {

        val existing = findViewing(viewingId, tenantId)
            ?: return@withContext BookingResult.Failure(
                "This viewing no longer exists."
            )

        val slotTime = ViewingTime.toMillis(date, slot)
            ?: return@withContext BookingResult.Failure("Invalid date or time.")

        if (slotTime <= System.currentTimeMillis()) {
            return@withContext BookingResult.InPast
        }

        if (isSlotBooked(existing.propertyId, date, slot, viewingId)) {
            return@withContext BookingResult.SlotTaken
        }

        if (database.tenantHasViewingAt(tenantId, date, slot, viewingId)) {
            return@withContext BookingResult.TenantBusy
        }

        if (database.updateViewing(viewingId, tenantId, date, slot)) {
            BookingResult.Success(
                viewingId,
                existing.propertyTitle,
                date,
                slot
            )
        } else {
            BookingResult.SlotTaken
        }
    }

    // DELETE: Cancel a viewing.
    suspend fun cancel(
        viewingId: Long,
        tenantId: Long
    ): Boolean = withContext(Dispatchers.IO) {
        database.deleteViewing(viewingId, tenantId)
    }

    // READ: Get a tenant's viewing requests, soonest first.
    suspend fun getViewings(
        tenantId: Long
    ): List<ViewingItem> = withContext(Dispatchers.IO) {

        getTenantViewings(tenantId).use { cursor ->
            val items = mutableListOf<ViewingItem>()

            while (cursor.moveToNext()) {
                items.add(toViewing(cursor))
            }

            items.sortedBy {
                ViewingTime.toMillis(it.date, it.timeSlot)
                    ?: Long.MAX_VALUE
            }
        }
    }

    // READ: Get one viewing.
    suspend fun getViewing(
        viewingId: Long,
        tenantId: Long
    ): ViewingItem? = withContext(Dispatchers.IO) {
        findViewing(viewingId, tenantId)
    }

    // Find a viewing belonging to a specific tenant.
    private fun findViewing(
        viewingId: Long,
        tenantId: Long
    ): ViewingItem? {
        return database.getViewingById(viewingId, tenantId).use { cursor ->
            if (cursor.moveToFirst()) {
                toViewing(cursor)
            } else {
                null
            }
        }
    }

    // Check whether a slot is already booked.
    private fun isSlotBooked(
        propertyId: Long,
        date: String,
        slot: String,
        excludeViewingId: Long
    ): Boolean {
        return database.getBookedSlots(
            propertyId,
            date,
            excludeViewingId
        ).use { cursor ->

            var found = false

            while (cursor.moveToNext()) {
                if (cursor.getString(0) == slot) {
                    found = true
                    break
                }
            }

            found
        }
    }

    // Convert a database row into a ViewingItem.
    private fun toViewing(cursor: Cursor): ViewingItem {
        return ViewingItem(
            id = cursor.getLong(
                cursor.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_VIEWING_ID
                )
            ),
            propertyId = cursor.getLong(
                cursor.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_PROPERTY_ID
                )
            ),
            propertyTitle = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_PROPERTY_TITLE
                )
            ),
            propertyLocation = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_PROPERTY_LOCATION
                )
            ),
            date = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_VIEWING_DATE
                )
            ),
            timeSlot = cursor.getString(
                cursor.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_VIEWING_SLOT
                )
            )
        )
    }

    // Access the existing tenant viewing query through the database helper.
    private fun getTenantViewings(tenantId: Long): Cursor {
        return database.getTenantViewings(tenantId)
    }
}
