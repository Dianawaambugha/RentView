package com.example.rentview.data

import android.database.Cursor
import com.example.rentview.UserDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Repository: the only class the ViewModels talk to for property data.
// It hides the SQLite cursors and returns plain Kotlin objects.
class PropertyRepository(private val database: UserDatabaseHelper) {

    // Every approved property, each one with its photo URIs attached.
    suspend fun getApprovedProperties(): List<PropertyItem> =
        withContext(Dispatchers.IO) {
            val properties = database.getApprovedProperties().use { cursor ->
                val items = mutableListOf<PropertyItem>()
                while (cursor.moveToNext()) {
                    items.add(toProperty(cursor))
                }
                items
            }

            // The first cursor is closed before the photo queries start.
            properties.map { it.copy(photoUris = readPhotoUris(it.id)) }
        }

    suspend fun getProperty(propertyId: Long): PropertyItem? =
        withContext(Dispatchers.IO) {
            database.getPropertyById(propertyId).use { cursor ->
                if (cursor.moveToFirst()) toProperty(cursor) else null
            }
        }

    suspend fun getPhotoCount(propertyId: Long): Int =
        withContext(Dispatchers.IO) {
            database.getPropertyPhotos(propertyId).use { it.count }
        }

    suspend fun getPhotoUris(propertyId: Long): List<String> =
        withContext(Dispatchers.IO) {
            readPhotoUris(propertyId)
        }

    // Testing only: see UserDatabaseHelper.seedSampleProperties().
    suspend fun seedSampleProperties(): Int =
        withContext(Dispatchers.IO) {
            database.seedSampleProperties()
        }

    // Must be called from a background thread.
    private fun readPhotoUris(propertyId: Long): List<String> {
        return database.getPropertyPhotos(propertyId).use { cursor ->
            val uris = mutableListOf<String>()
            while (cursor.moveToNext()) {
                uris.add(cursor.getString(0))
            }
            uris
        }
    }

    private fun toProperty(cursor: Cursor): PropertyItem {
        return PropertyItem(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COL_PROPERTY_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COL_PROPERTY_TITLE)),
            location = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COL_PROPERTY_LOCATION)),
            rent = cursor.getDouble(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COL_PROPERTY_RENT)),
            bedrooms = cursor.getInt(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COL_PROPERTY_BEDROOMS)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(UserDatabaseHelper.COL_PROPERTY_DESCRIPTION)),
            isApproved = cursor.getString(
                cursor.getColumnIndexOrThrow(UserDatabaseHelper.COL_PROPERTY_STATUS)
            ) == UserDatabaseHelper.STATUS_APPROVED
        )
    }
}
