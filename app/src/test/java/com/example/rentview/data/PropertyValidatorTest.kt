package com.example.rentview.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PropertyValidatorTest {

    private fun validate(
        title: String = "2BR Apartment",
        location: String = "Kilimani, Nairobi",
        rent: String = "65000",
        bedrooms: String = "2",
        description: String = "Spacious and bright."
    ) = PropertyValidator.validate(title, location, rent, bedrooms, description)

    private fun errorsOf(result: ValidationResult) =
        (result as ValidationResult.Invalid).errors

    @Test
    fun validInputIsAccepted() {
        val result = validate()
        assertTrue(result is ValidationResult.Valid)
        assertEquals(65000.0, (result as ValidationResult.Valid).property.rent, 0.0)
    }

    @Test
    fun valuesAreTrimmed() {
        val property = (validate(title = "  Bedsitter  ") as ValidationResult.Valid).property
        assertEquals("Bedsitter", property.title)
    }

    @Test
    fun zeroBedroomsIsAllowed() {
        assertTrue(validate(bedrooms = "0") is ValidationResult.Valid)
    }

    @Test
    fun blankTextFieldsAreRejected() {
        val errors = errorsOf(validate(title = "   ", location = "", description = " "))
        assertTrue(errors.containsKey(PropertyField.TITLE))
        assertTrue(errors.containsKey(PropertyField.LOCATION))
        assertTrue(errors.containsKey(PropertyField.DESCRIPTION))
    }

    @Test
    fun badRentIsRejected() {
        assertTrue(errorsOf(validate(rent = "0")).containsKey(PropertyField.RENT))
        assertTrue(errorsOf(validate(rent = "-500")).containsKey(PropertyField.RENT))
        assertTrue(errorsOf(validate(rent = "abc")).containsKey(PropertyField.RENT))
        assertTrue(errorsOf(validate(rent = "NaN")).containsKey(PropertyField.RENT))
        assertTrue(errorsOf(validate(rent = "10000001")).containsKey(PropertyField.RENT))
    }

    @Test
    fun badBedroomsAreRejected() {
        assertTrue(errorsOf(validate(bedrooms = "-1")).containsKey(PropertyField.BEDROOMS))
        assertTrue(errorsOf(validate(bedrooms = "2.5")).containsKey(PropertyField.BEDROOMS))
        assertTrue(errorsOf(validate(bedrooms = "21")).containsKey(PropertyField.BEDROOMS))
    }

    @Test
    fun overLongTitleIsRejected() {
        val max = "a".repeat(PropertyValidator.MAX_TITLE_LENGTH)
        assertTrue(validate(title = max) is ValidationResult.Valid)
        assertTrue(errorsOf(validate(title = max + "a")).containsKey(PropertyField.TITLE))
    }

    @Test
    fun photoCountRules() {
        assertEquals(
            "Please select at least one property photo.",
            PropertyValidator.validatePhotoCount(0, requireAtLeastOne = true)
        )
        assertNull(PropertyValidator.validatePhotoCount(0, requireAtLeastOne = false))
        assertNull(PropertyValidator.validatePhotoCount(PropertyValidator.MAX_PHOTOS, true))
        assertEquals(
            "You can upload at most ${PropertyValidator.MAX_PHOTOS} photos.",
            PropertyValidator.validatePhotoCount(PropertyValidator.MAX_PHOTOS + 1, true)
        )
    }

    @Test
    fun photoTypeAndSizeRules() {
        assertTrue(PropertyValidator.isAllowedPhotoType("image/jpeg"))
        assertFalse(PropertyValidator.isAllowedPhotoType("application/pdf"))
        assertFalse(PropertyValidator.isAllowedPhotoType(null))
        assertTrue(PropertyValidator.isPhotoSizeOk(PropertyValidator.MAX_PHOTO_BYTES))
        assertFalse(PropertyValidator.isPhotoSizeOk(PropertyValidator.MAX_PHOTO_BYTES + 1))
    }
}