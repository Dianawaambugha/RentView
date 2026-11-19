package com.example.rentview.ui.activities

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.SupabaseClient
import com.example.rentview.ui.ImageLoader
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class PropertyEditRequestsActivity : AppCompatActivity() {

    companion object {
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_APPROVED = "APPROVED"
        private const val STATUS_REJECTED = "REJECTED"
    }

    private val supabase by lazy {
        SupabaseClient.client
    }

    private lateinit var requestsContainer: LinearLayout

    private data class EditRequestDetails(
        val requestId: String,
        val propertyId: String,
        val ownerName: String,
        val ownerEmail: String,
        val currentTitle: String,
        val currentLocation: String,
        val currentRent: Double,
        val currentBedrooms: Int,
        val currentDescription: String,
        val proposedTitle: String,
        val proposedLocation: String,
        val proposedRent: Double,
        val proposedBedrooms: Int,
        val proposedDescription: String,
        val currentPhotoUrls: List<String>,
        val proposedPhotoUrls: List<String>,
        val proposedPhotoPaths: List<String>,
        val currentPhotoPaths: List<String>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "Property Edit Requests"
            textSize = 25f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        requestsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            addView(requestsContainer)
        }

        val backButton = Button(this).apply {
            text = "Back"
            setOnClickListener {
                finish()
            }
        }

        mainLayout.addView(title)

        mainLayout.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        mainLayout.addView(backButton)

        setContentView(mainLayout)
    }

    override fun onResume() {
        super.onResume()

        if (::requestsContainer.isInitialized) {
            displayEditRequests()
        }
    }

    private fun displayEditRequests() {
        requestsContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val pendingRequests = supabase
                    .from("property_edit_requests")
                    .select {
                        filter {
                            eq("edit_status", STATUS_PENDING)
                        }
                    }
                    .decodeList<JsonObject>()

                if (pendingRequests.isEmpty()) {
                    requestsContainer.addView(
                        TextView(this@PropertyEditRequestsActivity).apply {
                            text = "No pending property edit requests."
                            textSize = 17f
                            gravity = Gravity.CENTER
                            setPadding(0, 30, 0, 30)
                        }
                    )
                    return@launch
                }

                for (request in pendingRequests) {

                    val requestId = request["id"]
                        ?.jsonPrimitive?.content ?: continue

                    val propertyId = request["property_id"]
                        ?.jsonPrimitive?.content ?: continue

                    val propertyRows = supabase
                        .from("properties")
                        .select {
                            filter {
                                eq("id", propertyId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val property = propertyRows.firstOrNull()
                        ?: continue

                    val ownerId = property["owner_id"]
                        ?.jsonPrimitive?.content ?: ""

                    val ownerRows = supabase
                        .from("profiles")
                        .select {
                            filter {
                                eq("id", ownerId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val owner = ownerRows.firstOrNull()

                    val currentPhotoRows = supabase.from("property_photos")
                        .select {
                            filter {
                                eq("property_id", propertyId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val currentPhotoPaths = currentPhotoRows.mapNotNull {
                        it["photo_uri"]?.jsonPrimitive?.contentOrNull
                    }

                    val proposedPhotoRows = supabase.from("property_edit_photos")
                        .select {
                            filter {
                                eq("edit_id", requestId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val proposedPhotoPaths = proposedPhotoRows.mapNotNull {
                        it["photo_uri"]?.jsonPrimitive?.contentOrNull
                    }

                    val currentPhotoUrls = createSignedUrls(currentPhotoPaths)
                    val proposedPhotoUrls = createSignedUrls(proposedPhotoPaths)

                    val details = EditRequestDetails(
                        requestId = requestId,
                        propertyId = propertyId,
                        ownerName = owner?.get("full_name")
                            ?.jsonPrimitive?.content ?: "Unknown owner",
                        ownerEmail = owner?.get("email")
                            ?.jsonPrimitive?.content ?: "Unavailable",
                        currentTitle = property["title"]
                            ?.jsonPrimitive?.content ?: "",
                        currentLocation = property["location"]
                            ?.jsonPrimitive?.content ?: "",
                        currentRent = property["rent"]
                            ?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        currentBedrooms = property["bedrooms"]
                            ?.jsonPrimitive?.intOrNull ?: 0,
                        currentDescription = property["description"]
                            ?.jsonPrimitive?.content ?: "",
                        proposedTitle = request["proposed_title"]
                            ?.jsonPrimitive?.content ?: "",
                        proposedLocation = request["proposed_location"]
                            ?.jsonPrimitive?.content ?: "",
                        proposedRent = request["proposed_rent"]
                            ?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        proposedBedrooms = request["proposed_bedrooms"]
                            ?.jsonPrimitive?.intOrNull ?: 0,
                        proposedDescription = request["proposed_description"]
                            ?.jsonPrimitive?.content ?: "",
                        currentPhotoUrls = currentPhotoUrls,
                        proposedPhotoUrls = proposedPhotoUrls,
                        proposedPhotoPaths = proposedPhotoPaths,
                        currentPhotoPaths = currentPhotoPaths
                    )

                    addRequestCard(details)
                }

                if (requestsContainer.childCount == 0) {
                    requestsContainer.addView(
                        TextView(this@PropertyEditRequestsActivity).apply {
                            text = "No valid pending property edit requests were found."
                            textSize = 16f
                            gravity = Gravity.CENTER
                            setPadding(0, 30, 0, 30)
                        }
                    )
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@PropertyEditRequestsActivity,
                    "Unable to load edit requests: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun createSignedUrls(paths: List<String>): List<String> {
        return paths.mapNotNull { path ->
            try {
                supabase.storage
                    .from("property-photos")
                    .createSignedUrl(path, 3600.seconds)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun addPhotoSection(
        card: LinearLayout,
        heading: String,
        photoUrls: List<String>
    ) {

        val headingView = TextView(this).apply {
            text = heading
            textSize = 16f
            setTextColor(0xFF222222.toInt())
            setPadding(0, 16, 0, 8)
        }

        card.addView(headingView)

        if (photoUrls.isEmpty()) {
            card.addView(
                TextView(this).apply {
                    text = "No photos available."
                    textSize = 14f
                }
            )
            return
        }

        val horizontalScroll = android.widget.HorizontalScrollView(this)

        val photoContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        photoUrls.forEach { photoUrl ->

            val image = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(300, 240).apply {
                    marginEnd = 12
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            photoContainer.addView(image)

            ImageLoader.load(image, photoUrl, 900)
        }

        horizontalScroll.addView(photoContainer)
        card.addView(horizontalScroll)
    }

    private fun addRequestCard(request: EditRequestDetails) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(0xFFF1F1F1.toInt())
        }

        val details = TextView(this).apply {
            text = """
                PROPERTY EDIT REQUEST

                Property ID: ${request.propertyId}

                OWNER INFORMATION

                Owner Name: ${request.ownerName}
                Owner Email: ${request.ownerEmail}

                CURRENT PROPERTY INFORMATION

                Title: ${request.currentTitle}
                Location: ${request.currentLocation}
                Monthly Rent: KES ${formatRent(request.currentRent)}
                Bedrooms: ${request.currentBedrooms}
                Description: ${request.currentDescription}

                PROPOSED CHANGES

                Title: ${request.proposedTitle}
                Location: ${request.proposedLocation}
                Monthly Rent: KES ${formatRent(request.proposedRent)}
                Bedrooms: ${request.proposedBedrooms}
                Description: ${request.proposedDescription}

                Status: PENDING
            """.trimIndent()

            textSize = 15f
            setTextColor(0xFF222222.toInt())
        }

        card.addView(details)

        addPhotoSection(
            card,
            "CURRENT APPROVED PHOTOS",
            request.currentPhotoUrls
        )

        addPhotoSection(
            card,
            "PROPOSED REPLACEMENT PHOTOS",
            request.proposedPhotoUrls
        )

        if (request.proposedPhotoUrls.isEmpty()) {
            card.addView(
                TextView(this).apply {
                    text = "The owner did not request a photo replacement."
                    textSize = 13f
                    setPadding(0, 12, 0, 8)
                }
            )
        }

        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val approveButton = Button(this).apply {
            text = "Approve"
        }

        val rejectButton = Button(this).apply {
            text = "Reject"
        }

        approveButton.setOnClickListener {
            confirmDecision(request, true)
        }

        rejectButton.setOnClickListener {
            confirmDecision(request, false)
        }

        buttonLayout.addView(
            approveButton,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        buttonLayout.addView(
            rejectButton,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        card.addView(buttonLayout)

        val cardParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 20
        }

        requestsContainer.addView(card, cardParams)
    }

    private fun confirmDecision(
        request: EditRequestDetails,
        approve: Boolean
    ) {
        val action = if (approve) "approve" else "reject"

        AlertDialog.Builder(this)
            .setTitle("Confirm Decision")
            .setMessage("Are you sure you want to $action this property edit?")
            .setPositiveButton("Yes") { _, _ ->

                lifecycleScope.launch {
                    try {
                        if (approve) {

                            // Update the property's text details.
                            supabase.from("properties").update(
                                {
                                    set("title", request.proposedTitle)
                                    set("location", request.proposedLocation)
                                    set("rent", request.proposedRent)
                                    set("bedrooms", request.proposedBedrooms)
                                    set("description", request.proposedDescription)
                                }
                            ) {
                                filter {
                                    eq("id", request.propertyId)
                                }
                            }

                            // Replace photos only when the owner submitted new photos.
                            if (request.proposedPhotoPaths.isNotEmpty()) {

                                val insertedPaths = mutableListOf<String>()

                                try {
                                    // First, save the new photo records.
                                    for (path in request.proposedPhotoPaths) {

                                        val photoPayload = buildJsonObject {
                                            put("property_id", request.propertyId)
                                            put("photo_uri", path)
                                        }

                                        supabase.from("property_photos")
                                            .insert(photoPayload)

                                        insertedPaths.add(path)
                                    }

                                } catch (e: Exception) {

                                    // Roll back any new records if insertion fails.
                                    for (path in insertedPaths) {
                                        try {
                                            supabase.from("property_photos").delete {
                                                filter {
                                                    eq("property_id", request.propertyId)
                                                    eq("photo_uri", path)
                                                }
                                            }
                                        } catch (_: Exception) {
                                        }
                                    }

                                    throw e
                                }

                                // New photo records are saved.
                                // Delete only the old photo records.
                                for (oldPath in request.currentPhotoPaths) {
                                    supabase.from("property_photos").delete {
                                        filter {
                                            eq("property_id", request.propertyId)
                                            eq("photo_uri", oldPath)
                                        }
                                    }
                                }

                                // Remove temporary edit-photo records.
                                supabase.from("property_edit_photos").delete {
                                    filter {
                                        eq("edit_id", request.requestId)
                                    }
                                }

                                // Delete old Storage files after database replacement.
                                if (request.currentPhotoPaths.isNotEmpty()) {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            supabase.storage
                                                .from("property-photos")
                                                .delete(request.currentPhotoPaths)
                                        }
                                    } catch (_: Exception) {
                                        // The approved replacement remains valid if cleanup fails.
                                    }
                                }
                            }

                        } else {

                            // Rejected proposals must not affect approved property photos.
                            supabase.from("property_edit_photos").delete {
                                filter {
                                    eq("edit_id", request.requestId)
                                }
                            }

                            if (request.proposedPhotoPaths.isNotEmpty()) {
                                try {
                                    withContext(Dispatchers.IO) {
                                        supabase.storage
                                            .from("property-photos")
                                            .delete(request.proposedPhotoPaths)
                                    }
                                } catch (_: Exception) {
                                    // The request can still be rejected if Storage cleanup fails.
                                }
                            }
                        }

                        val newStatus = if (approve) {
                            STATUS_APPROVED
                        } else {
                            STATUS_REJECTED
                        }

                        supabase.from("property_edit_requests").update(
                            {
                                set("edit_status", newStatus)
                            }
                        ) {
                            filter {
                                eq("id", request.requestId)
                                eq("edit_status", STATUS_PENDING)
                            }
                        }

                        Toast.makeText(
                            this@PropertyEditRequestsActivity,
                            if (approve) {
                                "Property changes and any replacement photos approved."
                            } else {
                                "Property edit request rejected. Existing photos remain unchanged."
                            },
                            Toast.LENGTH_LONG
                        ).show()

                        displayEditRequests()

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@PropertyEditRequestsActivity,
                            "Request processing failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatRent(rent: Double): String {
        return String.format(Locale.getDefault(), "%,.2f", rent)
    }
}