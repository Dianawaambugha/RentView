package com.example.rentview.ui.activities

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.SupabaseClient
import com.example.rentview.ui.ImageLoader
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
import kotlin.time.Duration.Companion.seconds

class EditPropertyActivity : AppCompatActivity() {

    private var propertyId: String = ""

    private lateinit var titleInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var rentInput: EditText
    private lateinit var bedroomsInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var submitButton: Button

    private lateinit var choosePhotosButton: Button
    private lateinit var selectedPhotosText: TextView
    private lateinit var currentPhotosContainer: LinearLayout
    private lateinit var selectedPhotosContainer: LinearLayout

    private val selectedPhotos = mutableListOf<Uri>()

    private val uploadedPaths = mutableListOf<String>()

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
                    }

                    selectedPhotos.add(uri)
                }

                selectedPhotosText.text =
                    "${selectedPhotos.size} replacement photo(s) selected"

                displaySelectedPhotos()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_property)

        propertyId = intent.getStringExtra("PROPERTY_ID").orEmpty()

        titleInput = findViewById(R.id.editPropertyTitle)
        locationInput = findViewById(R.id.editPropertyLocation)
        rentInput = findViewById(R.id.editPropertyRent)
        bedroomsInput = findViewById(R.id.editPropertyBedrooms)
        descriptionInput = findViewById(R.id.editPropertyDescription)
        submitButton = findViewById(R.id.btnSubmitPropertyEdit)

        choosePhotosButton = findViewById(R.id.btnChooseReplacementPhotos)
        selectedPhotosText = findViewById(R.id.tvSelectedReplacementPhotos)
        currentPhotosContainer = findViewById(R.id.currentPropertyPhotos)
        selectedPhotosContainer = findViewById(R.id.selectedReplacementPhotos)

        if (propertyId.isBlank()) {
            Toast.makeText(
                this,
                "Property information is missing. Please reopen My Properties.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        loadPropertyDetails()

        choosePhotosButton.setOnClickListener {
            photoPicker.launch(arrayOf("image/*"))
        }

        submitButton.setOnClickListener {
            submitPropertyChanges()
        }
    }

    private fun loadPropertyDetails() {

        lifecycleScope.launch {

            try {

                val cloudUserId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("Please log in again.")

                val properties = supabase.from("properties")
                    .select {
                        filter {
                            eq("id", propertyId)
                            eq("owner_id", cloudUserId)
                        }
                    }
                    .decodeList<JsonObject>()

                val property = properties.firstOrNull()
                    ?: throw Exception("Property not found in your listings.")

                titleInput.setText(
                    property["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )

                locationInput.setText(
                    property["location"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )

                rentInput.setText(
                    property["rent"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )

                bedroomsInput.setText(
                    property["bedrooms"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )

                descriptionInput.setText(
                    property["description"]?.jsonPrimitive?.contentOrNull.orEmpty()
                )

                loadCurrentPhotos()

            } catch (e: Exception) {

                Toast.makeText(
                    this@EditPropertyActivity,
                    "Unable to load property: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
        }
    }

    private suspend fun loadCurrentPhotos() {

        currentPhotosContainer.removeAllViews()

        val photoRows = supabase.from("property_photos")
            .select {
                filter {
                    eq("property_id", propertyId)
                }
            }
            .decodeList<JsonObject>()

        for (row in photoRows) {

            val path = row["photo_uri"]
                ?.jsonPrimitive?.contentOrNull ?: continue

            val signedUrl = supabase.storage
                .from("property-photos")
                .createSignedUrl(path, 3600.seconds)

            val image = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(300, 240).apply {
                    marginEnd = 12
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            currentPhotosContainer.addView(image)

            ImageLoader.load(image, signedUrl, 900)
        }
    }

    private fun displaySelectedPhotos() {

        selectedPhotosContainer.removeAllViews()

        selectedPhotos.forEach { uri ->

            val image = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(300, 240).apply {
                    marginEnd = 12
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            selectedPhotosContainer.addView(image)

            ImageLoader.load(image, uri.toString(), 900)
        }
    }

    private fun submitPropertyChanges() {

        val title = titleInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val rentText = rentInput.text.toString().trim()
        val bedroomsText = bedroomsInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()

        if (
            title.isBlank() ||
            location.isBlank() ||
            rentText.isBlank() ||
            bedroomsText.isBlank() ||
            description.isBlank()
        ) {
            Toast.makeText(
                this,
                "Please fill in all fields.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rent = rentText.toDoubleOrNull()
        val bedrooms = bedroomsText.toIntOrNull()

        if (rent == null || rent <= 0) {
            rentInput.error = "Enter a valid rent greater than zero."
            return
        }

        if (bedrooms == null || bedrooms < 0) {
            bedroomsInput.error = "Enter a valid number of bedrooms."
            return
        }

        submitButton.isEnabled = false
        choosePhotosButton.isEnabled = false

        lifecycleScope.launch {

            var requestId: String? = null

            try {

                val cloudUserId = supabase.auth.currentUserOrNull()?.id
                    ?: throw Exception("Please log in again.")

                val properties = supabase.from("properties")
                    .select {
                        filter {
                            eq("id", propertyId)
                            eq("owner_id", cloudUserId)
                            eq("property_status", "APPROVED")
                        }
                    }
                    .decodeList<JsonObject>()

                if (properties.isEmpty()) {
                    throw Exception(
                        "This property is not approved or does not belong to your account."
                    )
                }

                val existingRequests = supabase.from("property_edit_requests")
                    .select {
                        filter {
                            eq("property_id", propertyId)
                            eq("edit_status", "PENDING")
                        }
                    }
                    .decodeList<JsonObject>()

                if (existingRequests.isNotEmpty()) {
                    throw Exception(
                        "This property already has an edit request awaiting admin approval."
                    )
                }

                val newRequestId = UUID.randomUUID().toString()
                requestId = newRequestId

                val requestPayload = buildJsonObject {
                    put("id", newRequestId)
                    put("property_id", propertyId)
                    put("proposed_title", title)
                    put("proposed_location", location)
                    put("proposed_rent", rent)
                    put("proposed_bedrooms", bedrooms)
                    put("proposed_description", description)
                    put("edit_status", "PENDING")
                }

                supabase.from("property_edit_requests")
                    .insert(requestPayload)

                if (selectedPhotos.isNotEmpty()) {

                    for (uri in selectedPhotos) {

                        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

                        val extension = when {
                            mimeType.contains("png") -> "png"
                            mimeType.contains("webp") -> "webp"
                            mimeType.contains("gif") -> "gif"
                            else -> "jpg"
                        }

                        val photoPath =
                            "$cloudUserId/$propertyId/edits/$newRequestId/${UUID.randomUUID()}.$extension"

                        val photoBytes = withContext(Dispatchers.IO) {
                            contentResolver.openInputStream(uri)?.use {
                                it.readBytes()
                            } ?: throw Exception("Could not read selected photo.")
                        }

                        withContext(Dispatchers.IO) {
                            supabase.storage
                                .from("property-photos")
                                .upload(photoPath, photoBytes)
                        }

                        uploadedPaths.add(photoPath)

                        val photoPayload = buildJsonObject {
                            put("edit_id", newRequestId)
                            put("photo_uri", photoPath)
                        }

                        supabase.from("property_edit_photos")
                            .insert(photoPayload)
                    }
                }

                Toast.makeText(
                    this@EditPropertyActivity,
                    if (selectedPhotos.isNotEmpty()) {
                        "Changes and replacement photos submitted for admin approval."
                    } else {
                        "Changes submitted for admin approval."
                    },
                    Toast.LENGTH_LONG
                ).show()

                finish()

            } catch (e: Exception) {

                if (requestId != null) {
                    try {
                        supabase.from("property_edit_photos").delete {
                            filter {
                                eq("edit_id", requestId!!)
                            }
                        }

                        supabase.from("property_edit_requests").delete {
                            filter {
                                eq("id", requestId!!)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }

                if (uploadedPaths.isNotEmpty()) {
                    try {
                        withContext(Dispatchers.IO) {
                            supabase.storage
                                .from("property-photos")
                                .delete(uploadedPaths.toList())
                        }
                    } catch (_: Exception) {
                    }
                }

                Toast.makeText(
                    this@EditPropertyActivity,
                    "Could not submit changes: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                submitButton.isEnabled = true
                choosePhotosButton.isEnabled = true
            }
        }
    }
}