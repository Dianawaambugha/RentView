package com.example.rentview.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Repository for property operations using Room.
class PropertyRepository(
    private val database: RentViewDatabase
) {

    private val propertyDao = database.propertyDao()
    private val photoDao = database.propertyPhotoDao()
    private val userDao = database.userDao()

    // READ: Get all approved properties with their photos.
    suspend fun getApprovedProperties(): List<PropertyItem> =
        withContext(Dispatchers.IO) {

            propertyDao.getApprovedProperties().map { property ->

                PropertyItem(
                    id = property.propertyId,
                    title = property.title,
                    location = property.location,
                    rent = property.rent,
                    bedrooms = property.bedrooms,
                    description = property.description,
                    isApproved = property.propertyStatus == "APPROVED",
                    photoUris = photoDao.getPhotoUris(property.propertyId)
                )
            }
        }

    // READ: Get one property.
    suspend fun getProperty(propertyId: Long): PropertyItem? =
        withContext(Dispatchers.IO) {

            val property = propertyDao.getById(propertyId)
                ?: return@withContext null

            PropertyItem(
                id = property.propertyId,
                title = property.title,
                location = property.location,
                rent = property.rent,
                bedrooms = property.bedrooms,
                description = property.description,
                isApproved = property.propertyStatus == "APPROVED"
            )
        }

    // READ: Count photos belonging to a property.
    suspend fun getPhotoCount(propertyId: Long): Int =
        withContext(Dispatchers.IO) {
            photoDao.countByProperty(propertyId)
        }

    // READ: Get photo URIs belonging to a property.
    suspend fun getPhotoUris(propertyId: Long): List<String> =
        withContext(Dispatchers.IO) {
            photoDao.getPhotoUris(propertyId)
        }

    // TESTING: Insert sample properties without creating duplicates.
    suspend fun seedSampleProperties(): Int =
        withContext(Dispatchers.IO) {

            // Do not insert samples if properties already exist.
            if (propertyDao.countAll() > 0) {
                return@withContext 0
            }
// Find an existing sample owner.
            var owner = userDao.getByEmail("sample.owner@rentview.test")

// Correct the role if this sample account already exists.
            if (owner != null && owner.role != "Property Owner") {
                owner = owner.copy(
                    role = "Property Owner",
                    approvalStatus = "APPROVED"
                )
                userDao.update(owner)
            }

// Create the sample owner only if the account does not exist.
            if (owner == null) {
                val newOwner = UserEntity(
                    fullName = "Sample Owner",
                    email = "sample.owner@rentview.test",
                    phone = "0700000000",
                    passwordHash = hashPassword("sample123"),
                    role = "Property Owner",
                    approvalStatus = "APPROVED"
                )

                val ownerId = userDao.insert(newOwner)

                if (ownerId == -1L) {
                    return@withContext 0
                }

                owner = userDao.getById(ownerId)
            }

            val ownerId = owner?.id ?: return@withContext 0

            // Match the existing SQLite sample properties.
            val samples = listOf(
                Triple(
                    "2BR Apartment in Kilimani",
                    "Kilimani, Nairobi",
                    65000.0
                ),
                Triple(
                    "Bedsitter near Strathmore",
                    "Madaraka, Nairobi",
                    15000.0
                ),
                Triple(
                    "3BR Maisonette",
                    "Lavington, Nairobi",
                    120000.0
                )
            )

            var inserted = 0

            for ((title, location, rent) in samples) {

                val property = PropertyEntity(
                    ownerId = ownerId,
                    title = title,
                    location = location,
                    rent = rent,
                    bedrooms = 2,
                    description = "Sample listing for testing",
                    propertyStatus = "APPROVED",
                    createdAt = currentTimestamp()
                )

                val propertyId = propertyDao.insert(property)

                if (propertyId != -1L) {
                    inserted++
                }
            }

            inserted
        }
    // SHA-256 password hashing helper.
    private fun hashPassword(password: String): String {

        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }

    // Generate a SQLite-compatible UTC timestamp.
    private fun currentTimestamp(): String {

        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.US
        )

        formatter.timeZone = TimeZone.getTimeZone("UTC")

        return formatter.format(Date())
    }
}