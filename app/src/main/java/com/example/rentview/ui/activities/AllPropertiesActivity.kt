package com.example.rentview.ui.activities

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import io.github.jan.supabase.storage.storage
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.SupabaseClient
import com.example.rentview.ui.ImageLoader
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

class AllPropertiesActivity : AppCompatActivity() {

    private lateinit var propertiesContainer: LinearLayout

    private val supabase by lazy {
        SupabaseClient.client
    }

    private data class PropertyDetails(
        val id: String,
        val title: String,
        val location: String,
        val rent: Double,
        val bedrooms: Int,
        val status: String,
        val ownerName: String,
        val ownerEmail: String,
        val createdAt: String,
        val viewingRequests: Int,
        val photoUrls: List<String>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "All Properties"
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
            setTextColor(0xFF222222.toInt())
        }

        propertiesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            addView(propertiesContainer)
        }

        val backButton = Button(this).apply {
            text = "Back to Dashboard"
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

        displayProperties()
    }

    private fun displayProperties() {

        propertiesContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val properties = supabase
                    .from("properties")
                    .select()
                    .decodeList<JsonObject>()

                val propertyDetails = mutableListOf<PropertyDetails>()

                for (property in properties) {

                    val propertyId = property["id"]
                        ?.jsonPrimitive?.content ?: continue

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

                    val viewingRows = supabase
                        .from("viewings")
                        .select {
                            filter {
                                eq("property_id", propertyId)
                            }
                        }
                        .decodeList<JsonObject>()

                    // Retrieve this property's photo records.
                    val photoRows = supabase
                        .from("property_photos")
                        .select {
                            filter {
                                eq("property_id", propertyId)
                            }
                        }
                        .decodeList<JsonObject>()

                    // Convert private Storage paths into temporary signed URLs.
                    val photoUrls = photoRows.mapNotNull { photo ->
                        val path = photo["photo_uri"]
                            ?.jsonPrimitive?.content
                            ?: return@mapNotNull null

                        try {
                            supabase.storage
                                .from("property-photos")
                                .createSignedUrl(path, 3600.seconds)
                        } catch (_: Exception) {
                            null
                        }
                    }

                    propertyDetails.add(
                        PropertyDetails(
                            id = propertyId,

                            title = property["title"]
                                ?.jsonPrimitive?.content
                                ?: "Unknown Property",

                            location = property["location"]
                                ?.jsonPrimitive?.content
                                ?: "Location unavailable",

                            rent = property["rent"]
                                ?.jsonPrimitive?.doubleOrNull ?: 0.0,

                            bedrooms = property["bedrooms"]
                                ?.jsonPrimitive?.intOrNull ?: 0,

                            status = property["property_status"]
                                ?.jsonPrimitive?.content
                                ?: "UNKNOWN",

                            ownerName = owner?.get("full_name")
                                ?.jsonPrimitive?.content
                                ?: "Unknown Owner",

                            ownerEmail = owner?.get("email")
                                ?.jsonPrimitive?.content
                                ?: "No email available",

                            createdAt = property["created_at"]
                                ?.jsonPrimitive?.content
                                ?: "Unknown",

                            viewingRequests = viewingRows.size,

                            photoUrls = photoUrls
                        )
                    )
                }

                propertiesContainer.removeAllViews()

                if (propertyDetails.isEmpty()) {
                    propertiesContainer.addView(
                        TextView(this@AllPropertiesActivity).apply {
                            text = "No properties have been listed yet."
                            textSize = 16f
                            gravity = Gravity.CENTER
                            setPadding(0, 30, 0, 30)
                        }
                    )

                    return@launch
                }

                propertyDetails.forEach { property ->
                    addPropertyCard(property)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@AllPropertiesActivity,
                    "Unable to load properties: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addPropertyCard(property: PropertyDetails) {

        val propertyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(0xFFF1F1F1.toInt())
        }

        // Display property photos.
        if (property.photoUrls.isNotEmpty()) {

            val photoScroll = HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = true
            }

            val photoStrip = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val density = resources.displayMetrics.density
            val imageWidth = (260 * density).toInt()
            val imageHeight = (180 * density).toInt()
            val gap = (8 * density).toInt()

            property.photoUrls.forEach { photoUrl ->

                val image = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        imageWidth,
                        imageHeight
                    ).apply {
                        marginEnd = gap
                    }

                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(Color.parseColor("#DCE5EC"))
                    contentDescription = "Property photo"
                }

                photoStrip.addView(image)

                ImageLoader.load(image, photoUrl, 900)
            }

            photoScroll.addView(photoStrip)

            propertyCard.addView(photoScroll)

        } else {
            propertyCard.addView(
                TextView(this).apply {
                    text = "No photos uploaded"
                    textSize = 14f
                    setPadding(0, 0, 0, 12)
                }
            )
        }

        val rentFormatted = String.format(
            Locale.US,
            "%,.2f",
            property.rent
        )

        val details = TextView(this).apply {
            text = """
                PROPERTY DETAILS

                Property ID: ${property.id}
                Title: ${property.title}
                Location: ${property.location}
                Monthly Rent: KES $rentFormatted
                Bedrooms: ${property.bedrooms}
                Status: ${property.status}

                OWNER INFORMATION

                Owner Name: ${property.ownerName}
                Owner Email: ${property.ownerEmail}

                LISTING INFORMATION

                Date Added: ${property.createdAt}
                Viewing Requests: ${property.viewingRequests}
            """.trimIndent()

            textSize = 16f
            setTextColor(0xFF222222.toInt())
            setPadding(0, 16, 0, 0)
        }

        propertyCard.addView(details)

        val cardParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 20
        }

        propertiesContainer.addView(propertyCard, cardParams)
    }
}