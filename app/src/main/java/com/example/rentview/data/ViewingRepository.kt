package com.example.rentview.data

import android.database.sqlite.SQLiteConstraintException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Repository for viewing operations using Room.
// All booking rules remain in this repository.
class ViewingRepository(
    private val database: RentViewDatabase,
    private val properties: PropertyRepository
) {

    private val viewingDao = database.viewingDao()

    // Get available slots for a property on a particular date.
    // Exclude the current viewing when rescheduling.
    suspend fun getAvailableSlots(
        propertyId: Long,
        date: String,
        excludeViewingId: Long = 0L
    ): List<String> = withContext(Dispatchers.IO) {

        val booked = viewingDao.getBookedSlots(
            propertyId,
            date,
            excludeViewingId
        ).toSet()

        val now = System.currentTimeMillis()

        ViewingTime.ALL_SLOTS.filter { slot ->
            val slotTime = ViewingTime.toMillis(date, slot)

            slot !in booked &&
                    slotTime != null &&
                    slotTime > now
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
            ?: return@withContext BookingResult.Failure(
                "Invalid date or time."
            )

        if (slotTime <= System.currentTimeMillis()) {
            return@withContext BookingResult.InPast
        }

        // A tenant can only book an approved property.
        val property = properties.getProperty(propertyId)

        if (property == null || !property.isApproved) {
            return@withContext BookingResult.PropertyUnavailable
        }

        // Check whether another tenant booked this property slot.
        if (
            viewingDao.countSlotConflicts(
                propertyId,
                date,
                slot,
                0L
            ) > 0
        ) {
            return@withContext BookingResult.SlotTaken
        }

        // Prevent the same tenant from booking two viewings
        // at the same date and time.
        if (
            viewingDao.countTenantConflicts(
                tenantId,
                date,
                slot,
                0L
            ) > 0
        ) {
            return@withContext BookingResult.TenantBusy
        }

        try {
            val viewingId = viewingDao.insert(
                ViewingEntity(
                    propertyId = propertyId,
                    tenantId = tenantId,
                    viewingDate = date,
                    viewingSlot = slot
                )
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

        } catch (e: SQLiteConstraintException) {
            BookingResult.SlotTaken
        }
    }

    // UPDATE: Reschedule an existing viewing.
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
            ?: return@withContext BookingResult.Failure(
                "Invalid date or time."
            )

        if (slotTime <= System.currentTimeMillis()) {
            return@withContext BookingResult.InPast
        }

        // Exclude this viewing from the conflict checks.
        if (
            viewingDao.countSlotConflicts(
                existing.propertyId,
                date,
                slot,
                viewingId
            ) > 0
        ) {
            return@withContext BookingResult.SlotTaken
        }

        if (
            viewingDao.countTenantConflicts(
                tenantId,
                date,
                slot,
                viewingId
            ) > 0
        ) {
            return@withContext BookingResult.TenantBusy
        }

        try {
            val updated = viewingDao.updateForTenant(
                viewingId,
                tenantId,
                date,
                slot
            )

            if (updated > 0) {
                BookingResult.Success(
                    viewingId,
                    existing.propertyTitle,
                    date,
                    slot
                )
            } else {
                BookingResult.SlotTaken
            }

        } catch (e: SQLiteConstraintException) {
            BookingResult.SlotTaken
        }
    }

    // DELETE: Cancel a viewing belonging to the specified tenant.
    suspend fun cancel(
        viewingId: Long,
        tenantId: Long
    ): Boolean = withContext(Dispatchers.IO) {

        viewingDao.deleteForTenant(viewingId, tenantId) > 0
    }

    // READ: Get all viewings belonging to a tenant.
    suspend fun getViewings(
        tenantId: Long
    ): List<ViewingItem> = withContext(Dispatchers.IO) {

        viewingDao.getByTenant(tenantId)
            .mapNotNull { viewing ->

                // Preserve the old INNER JOIN behaviour.
                val property = properties.getProperty(
                    viewing.propertyId
                ) ?: return@mapNotNull null

                ViewingItem(
                    id = viewing.viewingId,
                    propertyId = viewing.propertyId,
                    propertyTitle = property.title,
                    propertyLocation = property.location,
                    date = viewing.viewingDate,
                    timeSlot = viewing.viewingSlot
                )
            }
            .sortedBy {
                ViewingTime.toMillis(
                    it.date,
                    it.timeSlot
                ) ?: Long.MAX_VALUE
            }
    }

    // READ: Get one viewing belonging to a specific tenant.
    suspend fun getViewing(
        viewingId: Long,
        tenantId: Long
    ): ViewingItem? = withContext(Dispatchers.IO) {

        findViewing(viewingId, tenantId)
    }

    // Find a viewing and its associated property.
    private suspend fun findViewing(
        viewingId: Long,
        tenantId: Long
    ): ViewingItem? {

        val viewing = viewingDao.getById(
            viewingId,
            tenantId
        ) ?: return null

        // Match the old INNER JOIN behaviour.
        val property = properties.getProperty(
            viewing.propertyId
        ) ?: return null

        return ViewingItem(
            id = viewing.viewingId,
            propertyId = viewing.propertyId,
            propertyTitle = property.title,
            propertyLocation = property.location,
            date = viewing.viewingDate,
            timeSlot = viewing.viewingSlot
        )
    }
}