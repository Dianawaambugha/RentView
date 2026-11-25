package com.example.rentview.ui.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.PropertyField
import com.example.rentview.data.PropertyValidator
import com.example.rentview.data.SupabaseClient
import com.example.rentview.data.ValidationResult
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

class AddPropertyActivity : AppCompatActivity() {

    // A photo that has already been read and checked, ready to upload.
    private class PreparedPhoto(val bytes: ByteArray, val extension: String)

    private val selectedPhotos = mutableListOf<Uri>()

    private val supabase by lazy {
        SupabaseClient.client
    }

    private val photoPicker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->

            if (uris.isEmpty()) return@registerForActivityResult

            val countError = PropertyValidator.validatePhotoCount(
                uris.size,
                requireAtLeastOne = true
            )

            if (countError != null) {
                Toast.makeText(this, countError, Toast.LENGTH_LONG).show()
                return@registerForActivityResult
            }

            // Keep only real images (JPEG, PNG or WebP).
            val validUris = uris.filter {
                PropertyValidator.isAllowedPhotoType(contentResolver.getType(it))
            }

            if (validUris.size < uris.size) {
                Toast.makeText(
                    this,
                    "Only JPEG, PNG or WebP photos are allowed. Other files were skipped.",
                    Toast.LENGTH_LONG
                ).show()
            }

            selectedPhotos.clear()

            validUris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Permission may already be available.
                }

                selectedPhotos.add(uri)
            }

            findViewById<TextView>(R.id.tvSelectedPhotos).text =
                "${selectedPhotos.size} photo(s) selected"
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_property)

        val etTitle = findViewById<EditText>(R.id.etPropertyTitle)
        val etLocation = findViewById<EditText>(R.id.etPropertyLocation)
        val etRent = findViewById<EditText>(R.id.etPropertyRent)
        val etBedrooms = findViewById<EditText>(R.id.etPropertyBedrooms)
        val etDescription = findViewById<EditText>(R.id.etPropertyDescription)

        val btnChoosePhotos = findViewById<Button>(R.id.btnChoosePhotos)
        val btnSaveProperty = findViewById<Button>(R.id.btnSaveProperty)
        val btnCancelProperty = findViewById<Button>(R.id.btnCancelProperty)

        btnChoosePhotos.setOnClickListener {
            photoPicker.launch(arrayOf("image/*"))
        }

        btnCancelProperty.setOnClickListener {
            finish()
        }

        btnSaveProperty.setOnClickListener {

            // Clear errors from a previous attempt.
            listOf(etTitle, etLocation, etRent, etBedrooms, etDescription)
                .forEach { it.error = null }

            val result = PropertyValidator.validate(
                title = etTitle.text.toString(),
                location = etLocation.text.toString(),
                rentText = etRent.text.toString(),
                bedroomsText = etBedrooms.text.toString(),
                description = etDescription.text.toString()
            )

            if (result is ValidationResult.Invalid) {

                val fieldViews = mapOf(
                    PropertyField.TITLE to etTitle,
                    PropertyField.LOCATION to etLocation,
                    PropertyField.RENT to etRent,
                    PropertyField.BEDROOMS to etBedrooms,
                    PropertyField.DESCRIPTION to etDescription
                )

                result.errors.forEach { (field, message) ->
                    fieldViews[field]?.error = message
                }

                fieldViews[result.errors.keys.first()]?.requestFocus()
                return@setOnClickListener
            }

            val property = (result as ValidationResult.Valid).property

            val photoError = PropertyValidator.validatePhotoCount(
                selectedPhotos.size,
                requireAtLeastOne = true
            )

            if (photoError != null) {
                Toast.makeText(this, photoError, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnSaveProperty.isEnabled = false

            lifecycleScope.launch {

                var cloudPropertyId: String? = null
                var propertyInserted = false
                val uploadedPaths = mutableListOf<String>()

                try {

                    // Read and check every photo BEFORE saving anything,
                    // so a bad file never leaves a half-created listing.
                    val preparedPhotos = withContext(Dispatchers.IO) {
                        selectedPhotos.map { uri ->

                            val mimeType = contentResolver.getType(uri)

                            if (!PropertyValidator.isAllowedPhotoType(mimeType)) {
                                throw Exception("Only JPEG, PNG or WebP photos are allowed.")
                            }

                            val bytes = contentResolver.openInputStream(uri)?.use {
                                it.readBytes()
                            } ?: throw Exception("Could not read one of the selected photos.")

                            if (!PropertyValidator.isPhotoSizeOk(bytes.size)) {
                                throw Exception(
                                    "Each photo must be under " +
                                            "${PropertyValidator.MAX_PHOTO_BYTES / (1024 * 1024)} MB."
                                )
                            }

                            PreparedPhoto(bytes, PropertyValidator.photoExtension(mimeType))
                        }
                    }

                    // Identify the authenticated Supabase user.
                    val cloudUserId = supabase.auth.currentSessionOrNull()?.user?.id
                        ?: throw Exception("Please log in again before adding a property.")

                    // Read the authoritative owner profile from Supabase.
                    val profiles = supabase.from("profiles")
                        .select {
                            filter {
                                eq("id", cloudUserId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val profile = profiles.firstOrNull()
                        ?: throw Exception("Your cloud user profile could not be found.")

                    val cloudRole = profile["role"]?.jsonPrimitive?.contentOrNull
                    val cloudApprovalStatus =
                        profile["approval_status"]?.jsonPrimitive?.contentOrNull

                    if (cloudRole != "PROPERTY_OWNER") {
                        throw Exception("Only property owners can add properties.")
                    }

                    if (cloudApprovalStatus != "APPROVED") {
                        throw Exception("Your owner account is awaiting admin approval.")
                    }

                    val newPropertyId = UUID.randomUUID().toString()
                    cloudPropertyId = newPropertyId

                    // New listings always start as PENDING until an admin approves them.
                    val propertyPayload = buildJsonObject {
                        put("id", newPropertyId)
                        put("owner_id", cloudUserId)
                        put("title", property.title)
                        put("location", property.location)
                        put("rent", property.rent)
                        put("bedrooms", property.bedrooms)
                        put("description", property.description)
                        put("property_status", "PENDING")
                        put("created_at", java.time.Instant.now().toString())
                    }

                    withContext(Dispatchers.IO) {
                        supabase.from("properties").insert(propertyPayload)
                    }
                    propertyInserted = true

                    // Upload the photos and save their Storage paths.
                    for (photo in preparedPhotos) {

                        val photoPath =
                            "$cloudUserId/$newPropertyId/${UUID.randomUUID()}.${photo.extension}"

                        withContext(Dispatchers.IO) {
                            supabase.storage
                                .from("property-photos")
                                .upload(photoPath, photo.bytes)
                        }
                        uploadedPaths.add(photoPath)

                        val photoPayload = buildJsonObject {
                            put("property_id", newPropertyId)
                            put("photo_uri", photoPath)
                        }

                        withContext(Dispatchers.IO) {
                            supabase.from("property_photos").insert(photoPayload)
                        }
                    }

                    Toast.makeText(
                        this@AddPropertyActivity,
                        "Property submitted successfully. Waiting for admin approval.",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()

                } catch (e: Exception) {

                    // Undo anything saved so far so no half-created listing is left behind.
                    rollback(cloudPropertyId, propertyInserted, uploadedPaths)

                    Toast.makeText(
                        this@AddPropertyActivity,
                        "Property submission failed: ${e.message ?: "Please try again."}",
                        Toast.LENGTH_LONG
                    ).show()

                } finally {
                    btnSaveProperty.isEnabled = true
                }
            }
        }
    }

    // Best effort: each step is wrapped so one failure does not stop the others.
    private suspend fun rollback(
        propertyId: String?,
        propertyInserted: Boolean,
        uploadedPaths: List<String>
    ) {
        if (propertyId == null) return

        withContext(Dispatchers.IO) {

            if (uploadedPaths.isNotEmpty()) {
                try {
                    supabase.storage.from("property-photos").delete(uploadedPaths)
                } catch (_: Exception) {
                }
            }

            if (propertyInserted) {
                try {
                    supabase.from("property_photos").delete {
                        filter {
                            eq("property_id", propertyId)
                        }
                    }
                } catch (_: Exception) {
                }

                try {
                    supabase.from("properties").delete {
                        filter {
                            eq("id", propertyId)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}