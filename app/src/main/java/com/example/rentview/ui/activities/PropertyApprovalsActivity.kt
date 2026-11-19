package com.example.rentview.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.SupabaseClient
import com.example.rentview.ui.ImageLoader
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.seconds
import java.util.Locale

class PropertyApprovalsActivity : AppCompatActivity() {

    companion object {
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_APPROVED = "APPROVED"
        private const val STATUS_REJECTED = "REJECTED"
        private const val ROLE_ADMIN = "ADMIN"
    }

    private lateinit var propertyContainer: LinearLayout

    private val supabase by lazy {
        SupabaseClient.client
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(android.graphics.Color.rgb(245, 247, 250))
        }

        val heading = TextView(this).apply {
            text = "Review Property Listings"
            textSize = 24f
            setTextColor(android.graphics.Color.rgb(23, 59, 87))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        propertyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            addView(propertyContainer)
        }

        rootLayout.addView(heading)

        rootLayout.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val backButton = Button(this).apply {
            text = "Back to Dashboard"
            isAllCaps = false

            setOnClickListener {
                startActivity(
                    Intent(
                        this@PropertyApprovalsActivity,
                        AdminDashboardActivity::class.java
                    )
                )
                finish()
            }
        }

        rootLayout.addView(
            backButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(rootLayout)
    }

    override fun onResume() {
        super.onResume()

        if (::propertyContainer.isInitialized) {
            loadPendingProperties()
        }
    }

    private suspend fun requireAdmin() {

        val adminId = supabase.auth.currentUserOrNull()?.id
            ?: throw Exception("Please log in again.")

        val profiles = supabase.from("profiles")
            .select {
                filter {
                    eq("id", adminId)
                }
            }
            .decodeList<JsonObject>()

        val role = profiles.firstOrNull()
            ?.get("role")
            ?.jsonPrimitive
            ?.contentOrNull

        if (role != ROLE_ADMIN) {
            throw Exception("Administrator access is required.")
        }
    }

    private fun loadPendingProperties() {

        propertyContainer.removeAllViews()

        lifecycleScope.launch {

            try {

                requireAdmin()

                val pendingProperties = supabase.from("properties")
                    .select {
                        filter {
                            eq("property_status", STATUS_PENDING)
                        }
                    }
                    .decodeList<JsonObject>()

                if (pendingProperties.isEmpty()) {

                    val emptyMessage = TextView(
                        this@PropertyApprovalsActivity
                    ).apply {
                        text = "No property listings are waiting for review."
                        textSize = 17f
                        gravity = Gravity.CENTER
                        setPadding(12, 32, 12, 32)
                    }

                    propertyContainer.addView(emptyMessage)
                    return@launch
                }

                for (property in pendingProperties) {

                    val propertyId = property["id"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?: continue

                    val ownerId = property["owner_id"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?: ""

                    val title = property["title"]
                        ?.jsonPrimitive
                        ?.contentOrNull.orEmpty()

                    val location = property["location"]
                        ?.jsonPrimitive
                        ?.contentOrNull.orEmpty()

                    val rent = property["rent"]
                        ?.jsonPrimitive
                        ?.contentOrNull?.toDoubleOrNull() ?: 0.0

                    val bedrooms = property["bedrooms"]
                        ?.jsonPrimitive
                        ?.contentOrNull?.toIntOrNull() ?: 0

                    val description = property["description"]
                        ?.jsonPrimitive
                        ?.contentOrNull.orEmpty()

                    val ownerProfiles = supabase.from("profiles")
                        .select {
                            filter {
                                eq("id", ownerId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val owner = ownerProfiles.firstOrNull()

                    val ownerName = owner?.get("full_name")
                        ?.jsonPrimitive
                        ?.contentOrNull ?: "Unknown owner"

                    val ownerEmail = owner?.get("email")
                        ?.jsonPrimitive
                        ?.contentOrNull ?: "Unavailable"

                    val photoRows = supabase.from("property_photos")
                        .select {
                            filter {
                                eq("property_id", propertyId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val photoUrls = mutableListOf<String>()

                    for (photo in photoRows) {

                        val photoPath = photo["photo_uri"]
                            ?.jsonPrimitive
                            ?.contentOrNull ?: continue

                        try {

                            val signedUrl = supabase.storage
                                .from("property-photos")
                                .createSignedUrl(photoPath, 3600.seconds)

                            photoUrls.add(signedUrl)

                        } catch (_: Exception) {
                            // Continue loading other photos.
                        }
                    }

                    addPropertyCard(
                        propertyId = propertyId,
                        title = title,
                        location = location,
                        rent = rent,
                        bedrooms = bedrooms,
                        description = description,
                        ownerName = ownerName,
                        ownerEmail = ownerEmail,
                        photoUris = photoUrls
                    )
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@PropertyApprovalsActivity,
                    "Unable to load properties: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addPropertyCard(
        propertyId: String,
        title: String,
        location: String,
        rent: Double,
        bedrooms: Int,
        description: String,
        ownerName: String,
        ownerEmail: String,
        photoUris: List<String>
    ) {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        if (photoUris.isNotEmpty()) {

            val photoHeading = TextView(this).apply {
                text = "Property Photos"
                textSize = 18f
                setPadding(0, 0, 0, 12)
            }

            card.addView(photoHeading)

            photoUris.forEach { photoUri ->

                val propertyImage = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        220
                    ).apply {
                        bottomMargin = 12
                    }

                    scaleType = ImageView.ScaleType.CENTER_CROP
                }

                card.addView(propertyImage)

                ImageLoader.load(
                    propertyImage,
                    photoUri,
                    600
                )
            }

        } else {

            val noPhotoMessage = TextView(this).apply {
                text = "No property photos uploaded."
                textSize = 14f
                setPadding(0, 0, 0, 12)
            }

            card.addView(noPhotoMessage)
        }

        val details = TextView(this).apply {
            text = """
                Property: $title

                Location: $location

                Monthly Rent: KSh ${String.format(Locale.getDefault(), "%,.2f", rent)}

                Bedrooms: $bedrooms

                Description: $description

                Owner: $ownerName

                Owner Email: $ownerEmail

                Status: PENDING REVIEW
            """.trimIndent()

            textSize = 16f
            setTextColor(android.graphics.Color.rgb(38, 55, 70))
            setPadding(0, 0, 0, 16)
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val approveButton = Button(this).apply {
            text = "Approve"
            isAllCaps = false
        }

        val rejectButton = Button(this).apply {
            text = "Reject"
            isAllCaps = false
        }

        buttonRow.addView(
            approveButton,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        buttonRow.addView(
            rejectButton,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        card.addView(details)
        card.addView(buttonRow)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        cardParams.bottomMargin = 20

        propertyContainer.addView(card, cardParams)

        approveButton.setOnClickListener {
            confirmPropertyDecision(
                propertyId,
                title,
                STATUS_APPROVED
            )
        }

        rejectButton.setOnClickListener {
            confirmPropertyDecision(
                propertyId,
                title,
                STATUS_REJECTED
            )
        }
    }

    private fun confirmPropertyDecision(
        propertyId: String,
        title: String,
        status: String
    ) {

        val action = if (status == STATUS_APPROVED) {
            "approve"
        } else {
            "reject"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Decision")
            .setMessage("Are you sure you want to $action \"$title\"?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->

                lifecycleScope.launch {

                    try {

                        requireAdmin()

                        val updatePayload = buildJsonObject {
                            put("property_status", status)
                        }

                        supabase.from("properties")
                            .update(updatePayload) {
                                filter {
                                    eq("id", propertyId)
                                    eq("property_status", STATUS_PENDING)
                                }
                            }

                        val updatedProperties = supabase.from("properties")
                            .select {
                                filter {
                                    eq("id", propertyId)
                                    eq("property_status", status)
                                }
                            }
                            .decodeList<JsonObject>()

                        if (updatedProperties.isNotEmpty()) {

                            val message = if (status == STATUS_APPROVED) {
                                "Property approved successfully."
                            } else {
                                "Property rejected successfully."
                            }

                            Toast.makeText(
                                this@PropertyApprovalsActivity,
                                message,
                                Toast.LENGTH_LONG
                            ).show()

                        } else {

                            Toast.makeText(
                                this@PropertyApprovalsActivity,
                                "Property status was not updated. Check administrator permissions.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        loadPendingProperties()

                    } catch (e: Exception) {

                        Toast.makeText(
                            this@PropertyApprovalsActivity,
                            "Update failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }
}