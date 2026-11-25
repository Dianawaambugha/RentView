package com.example.rentview.data

enum class PropertyField { TITLE, LOCATION, RENT, BEDROOMS, DESCRIPTION, PHOTOS }

data class ValidatedProperty(
    val title: String,
    val location: String,
    val rent: Double,
    val bedrooms: Int,
    val description: String
)

sealed class ValidationResult {
    data class Valid(val property: ValidatedProperty) : ValidationResult()
    data class Invalid(val errors: Map<PropertyField, String>) : ValidationResult()
}

object PropertyValidator {

    const val MAX_TITLE_LENGTH = 100
    const val MAX_LOCATION_LENGTH = 100
    const val MAX_DESCRIPTION_LENGTH = 1000
    const val MAX_RENT = 10_000_000.0
    const val MAX_BEDROOMS = 20
    const val MAX_PHOTOS = 8
    const val MAX_PHOTO_BYTES = 5 * 1024 * 1024

    private val ALLOWED_PHOTO_TYPES = setOf("image/jpeg", "image/png", "image/webp")

    fun validate(
        title: String,
        location: String,
        rentText: String,
        bedroomsText: String,
        description: String
    ): ValidationResult {

        val errors = linkedMapOf<PropertyField, String>()

        val cleanTitle = title.trim()
        val cleanLocation = location.trim()
        val cleanDescription = description.trim()

        if (cleanTitle.isEmpty()) {
            errors[PropertyField.TITLE] = "Enter a title."
        } else if (cleanTitle.length > MAX_TITLE_LENGTH) {
            errors[PropertyField.TITLE] = "Title must be $MAX_TITLE_LENGTH characters or fewer."
        }

        if (cleanLocation.isEmpty()) {
            errors[PropertyField.LOCATION] = "Enter a location."
        } else if (cleanLocation.length > MAX_LOCATION_LENGTH) {
            errors[PropertyField.LOCATION] =
                "Location must be $MAX_LOCATION_LENGTH characters or fewer."
        }

        if (cleanDescription.isEmpty()) {
            errors[PropertyField.DESCRIPTION] = "Enter a description."
        } else if (cleanDescription.length > MAX_DESCRIPTION_LENGTH) {
            errors[PropertyField.DESCRIPTION] =
                "Description must be $MAX_DESCRIPTION_LENGTH characters or fewer."
        }

        val rent = rentText.trim().toDoubleOrNull()
        if (rent == null || rent.isNaN() || rent.isInfinite() || rent <= 0) {
            errors[PropertyField.RENT] = "Enter a rent greater than zero."
        } else if (rent > MAX_RENT) {
            errors[PropertyField.RENT] = "Rent is too high."
        }

        val bedrooms = bedroomsText.trim().toIntOrNull()
        if (bedrooms == null || bedrooms < 0 || bedrooms > MAX_BEDROOMS) {
            errors[PropertyField.BEDROOMS] = "Enter bedrooms between 0 and $MAX_BEDROOMS."
        }

        if (errors.isNotEmpty()) return ValidationResult.Invalid(errors)

        return ValidationResult.Valid(
            ValidatedProperty(
                title = cleanTitle,
                location = cleanLocation,
                rent = rent!!,
                bedrooms = bedrooms!!,
                description = cleanDescription
            )
        )
    }

    fun validatePhotoCount(count: Int, requireAtLeastOne: Boolean): String? = when {
        requireAtLeastOne && count == 0 -> "Please select at least one property photo."
        count > MAX_PHOTOS -> "You can upload at most $MAX_PHOTOS photos."
        else -> null
    }

    fun isAllowedPhotoType(mimeType: String?): Boolean =
        mimeType?.lowercase() in ALLOWED_PHOTO_TYPES

    fun isPhotoSizeOk(sizeBytes: Int): Boolean =
        sizeBytes in 1..MAX_PHOTO_BYTES

    fun photoExtension(mimeType: String?): String = when (mimeType?.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> "jpg"
    }
}