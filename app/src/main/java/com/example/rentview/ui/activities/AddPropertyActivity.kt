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
import com.example.rentview.data.SupabaseClient
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

    private val selectedPhotos = mutableListOf<Uri>()

    private val supabase by lazy {
        SupabaseClient.client
    }

    private val photoPicker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->

            if (uris.isNotEmpty()) {
                selectedPhotos.clear()

                uris.forEach { uri ->
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

            val title = etTitle.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val rent = etRent.text.toString().trim().toDoubleOrNull()
            val bedrooms = etBedrooms.text.toString().trim().toIntOrNull()
            val description = etDescription.text.toString().trim()

            if (
                title.isEmpty() ||
                location.isEmpty() ||
                rent == null ||
                rent <= 0 ||
                bedrooms == null ||
                bedrooms < 0 ||
                description.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    "Please enter valid property details.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (selectedPhotos.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please select at least one property photo.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            btnSaveProperty.isEnabled = false

            lifecycleScope.launch {

                try {

                    // Identify the authenticated Supabase user.
                    val cloudUserId = supabase.auth.currentSessionOrNull()?.user?.id
                        ?: throw Exception(
                            "Please log in again before adding a property."
                        )

                    // Read the authoritative owner profile from Supabase.
                    val profiles = supabase.from("profiles")
                        .select {
                            filter {
                                eq("id", cloudUserId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val profile = profiles.firstOrNull()
                        ?: throw Exception(
                            "Your cloud user profile could not be found."
                        )

                    val cloudRole = profile["role"]
                        ?.jsonPrimitive?.contentOrNull

                    val cloudApprovalStatus = profile["approval_status"]
                        ?.jsonPrimitive?.contentOrNull

                    if (cloudRole != "PROPERTY_OWNER") {
                        throw Exception(
                            "Only property owners can add properties."
                        )
                    }

                    if (cloudApprovalStatus != "APPROVED") {
                        throw Exception(
                            "Your owner account is awaiting admin approval."
                        )
                    }

                    val cloudPropertyId = UUID.randomUUID().toString()
                    val createdAt = java.time.Instant.now().toString()

                    // Insert the property into Supabase.
                    val propertyPayload = buildJsonObject {
                        put("id", cloudPropertyId)
                        put("owner_id", cloudUserId)
                        put("title", title)
                        put("location", location)
                        put("rent", rent)
                        put("bedrooms", bedrooms)
                        put("description", description)
                        put("property_status", "PENDING")
                        put("created_at", createdAt)
                    }

                    withContext(Dispatchers.IO) {
                        supabase.from("properties").insert(propertyPayload)
                    }

                    // Upload selected photos and save their Storage paths.
                    for (uri in selectedPhotos) {

                        val mimeType = contentResolver.getType(uri)
                            ?: "image/jpeg"

                        val extension = when (mimeType.lowercase()) {
                            "image/png" -> "png"
                            "image/webp" -> "webp"
                            "image/gif" -> "gif"
                            else -> "jpg"
                        }

                        val photoPath =
                            "$cloudUserId/$cloudPropertyId/${UUID.randomUUID()}.$extension"

                        val photoBytes = withContext(Dispatchers.IO) {
                            contentResolver.openInputStream(uri)?.use {
                                it.readBytes()
                            }
                        } ?: throw Exception(
                            "Could not read one of the selected photos."
                        )

                        withContext(Dispatchers.IO) {
                            supabase.storage
                                .from("property-photos")
                                .upload(photoPath, photoBytes)
                        }

                        val photoPayload = buildJsonObject {
                            put("property_id", cloudPropertyId)
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
}