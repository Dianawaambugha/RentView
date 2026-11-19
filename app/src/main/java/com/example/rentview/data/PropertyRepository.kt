package com.example.rentview.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

class PropertyRepository {

    private val client = SupabaseClient.client

    private fun JsonObject.value(key: String): String {
        return this[key]?.jsonPrimitive?.content ?: ""
    }

    private fun JsonObject.longValue(key: String): Long {
        return value(key).toLongOrNull() ?: 0L
    }

    private fun JsonObject.doubleValue(key: String): Double {
        return value(key).toDoubleOrNull() ?: 0.0
    }

    private suspend fun getCloudPhotos(propertyId: String): List<String> {
        val photos = client.from("property_photos")
            .select()
            .decodeList<JsonObject>()
            .filter { it.value("property_id") == propertyId }

        return photos.mapNotNull { photo ->
            val path = photo.value("photo_uri")

            if (path.isBlank()) {
                null
            } else {
                try {
                    client.storage.from("property-photos")
                        .createSignedUrl(path, 3600.seconds)
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    suspend fun getApprovedProperties(): List<PropertyItem> =
        withContext(Dispatchers.IO) {

            val cloudProperties = client.from("properties")
                .select()
                .decodeList<JsonObject>()
                .filter { it.value("property_status") == "APPROVED" }

            cloudProperties.map { property ->
                val cloudId = property.value("id")

                PropertyItem(
                    id = -1L,
                    title = property.value("title"),
                    location = property.value("location"),
                    rent = property.doubleValue("rent"),
                    bedrooms = property.longValue("bedrooms").toInt(),
                    description = property.value("description"),
                    isApproved = true,
                    photoUris = getCloudPhotos(cloudId),
                    supabaseId = cloudId
                )
            }
        }

    suspend fun getProperty(propertyId: String): PropertyItem? =
        withContext(Dispatchers.IO) {

            if (propertyId.isBlank()) return@withContext null

            val property = client.from("properties")
                .select {
                    filter {
                        eq("id", propertyId)
                    }
                }
                .decodeList<JsonObject>()
                .firstOrNull()
                ?: return@withContext null

            PropertyItem(
                id = -1L,
                title = property.value("title"),
                location = property.value("location"),
                rent = property.doubleValue("rent"),
                bedrooms = property.longValue("bedrooms").toInt(),
                description = property.value("description"),
                isApproved = property.value("property_status") == "APPROVED",
                photoUris = getCloudPhotos(propertyId),
                supabaseId = propertyId
            )
        }

    suspend fun getPhotoUris(propertyId: String): List<String> =
        withContext(Dispatchers.IO) {
            getCloudPhotos(propertyId)
        }
}