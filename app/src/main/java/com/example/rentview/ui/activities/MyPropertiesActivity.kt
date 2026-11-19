package com.example.rentview.ui.activities

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
import com.example.rentview.R
import com.example.rentview.data.SupabaseClient
import com.example.rentview.ui.ImageLoader
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

class MyPropertiesActivity : AppCompatActivity() {

    companion object {
        private const val STATUS_APPROVED = "APPROVED"
        private const val STATUS_PENDING = "PENDING"
    }

    private lateinit var propertiesContainer: LinearLayout

    private val supabase by lazy {
        SupabaseClient.client
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)

        propertiesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        scrollView.addView(propertiesContainer)
        setContentView(scrollView)
    }

    override fun onResume() {
        super.onResume()

        if (::propertiesContainer.isInitialized) {
            displayProperties()
        }
    }

    private fun displayProperties() {

        lifecycleScope.launch {
            propertiesContainer.removeAllViews()

            try {
                val cloudUserId = supabase.auth
                    .currentSessionOrNull()
                    ?.user
                    ?.id
                    ?: throw Exception("Please log in again.")

                val propertyRows = supabase
                    .from("properties")
                    .select {
                        filter {
                            eq("owner_id", cloudUserId)
                        }
                    }
                    .decodeList<JsonObject>()

                val heading = TextView(this@MyPropertiesActivity).apply {
                    text = getString(R.string.my_properties_title)
                    textSize = 26f
                    setPadding(0, 0, 0, 24)
                }

                propertiesContainer.addView(heading)

                if (propertyRows.isEmpty()) {

                    val emptyMessage =
                        TextView(this@MyPropertiesActivity).apply {
                            text = getString(R.string.my_properties_empty)
                            textSize = 18f
                            gravity = Gravity.CENTER
                        }

                    propertiesContainer.addView(emptyMessage)

                } else {

                    for (property in propertyRows) {

                        val propertyId = property["id"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            ?: continue

                        val title = property["title"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            .orEmpty()

                        val location = property["location"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            .orEmpty()

                        val rent = property["rent"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            ?.toDoubleOrNull() ?: 0.0

                        val bedrooms = property["bedrooms"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            ?.toIntOrNull() ?: 0

                        val description = property["description"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            .orEmpty()

                        val status = property["property_status"]
                            ?.jsonPrimitive
                            ?.contentOrNull
                            .orEmpty()

                        val pendingRequests = supabase
                            .from("property_edit_requests")
                            .select {
                                filter {
                                    eq("property_id", propertyId)
                                    eq("edit_status", STATUS_PENDING)
                                }
                            }
                            .decodeList<JsonObject>()

                        val hasPendingEdit = pendingRequests.isNotEmpty()

                        val photoRows = supabase
                            .from("property_photos")
                            .select {
                                filter {
                                    eq("property_id", propertyId)
                                }
                            }
                            .decodeList<JsonObject>()

                        val photoUrls = mutableListOf<String>()

                        for (photo in photoRows) {

                            val path = photo["photo_uri"]
                                ?.jsonPrimitive
                                ?.contentOrNull
                                ?: continue

                            try {
                                val signedUrl = supabase.storage
                                    .from("property-photos")
                                    .createSignedUrl(path, 3600.seconds)

                                photoUrls.add(signedUrl)

                            } catch (_: Exception) {
                                // Continue displaying the remaining photos.
                            }
                        }

                        val propertyCard =
                            LinearLayout(this@MyPropertiesActivity).apply {
                                orientation = LinearLayout.VERTICAL
                                setPadding(20, 20, 20, 20)
                                elevation = 4f
                                setBackgroundColor(0xFFF2F2F2.toInt())
                            }

                        photoUrls.forEach { photoUrl ->

                            val propertyImage =
                                ImageView(this@MyPropertiesActivity).apply {
                                    layoutParams = LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        220
                                    ).apply {
                                        bottomMargin = 12
                                    }

                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                }

                            propertyCard.addView(propertyImage)

                            ImageLoader.load(
                                propertyImage,
                                photoUrl,
                                600
                            )
                        }

                        val formattedRent = String.format(
                            java.util.Locale.getDefault(),
                            "%,.2f",
                            rent
                        )

                        val propertyDetails =
                            TextView(this@MyPropertiesActivity).apply {
                                text = getString(
                                    R.string.property_card_details,
                                    title,
                                    location,
                                    formattedRent,
                                    bedrooms,
                                    description,
                                    status
                                )

                                textSize = 16f
                            }

                        propertyCard.addView(propertyDetails)

                        if (hasPendingEdit) {

                            val pendingMessage =
                                TextView(this@MyPropertiesActivity).apply {
                                    text = getString(
                                        R.string.property_edit_pending
                                    )
                                    textSize = 14f
                                    setPadding(0, 12, 0, 12)
                                }

                            propertyCard.addView(pendingMessage)
                        }

                        if (status == STATUS_APPROVED) {

                            val editButton =
                                Button(this@MyPropertiesActivity).apply {
                                    text = getString(R.string.edit_property)
                                }

                            editButton.setOnClickListener {

                                if (hasPendingEdit) {

                                    Toast.makeText(
                                        this@MyPropertiesActivity,
                                        getString(
                                            R.string.property_edit_pending
                                        ),
                                        Toast.LENGTH_LONG
                                    ).show()

                                } else {

                                    val editIntent = Intent(
                                        this@MyPropertiesActivity,
                                        EditPropertyActivity::class.java
                                    )

                                    // This is the Supabase UUID, not a Room Long ID.
                                    editIntent.putExtra(
                                        "PROPERTY_ID",
                                        propertyId
                                    )

                                    editIntent.putExtra(
                                        "PROPERTY_ID_STRING",
                                        propertyId
                                    )

                                    startActivity(editIntent)
                                }
                            }

                            propertyCard.addView(editButton)
                        }

                        propertiesContainer.addView(
                            propertyCard,
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = 24
                            }
                        )
                    }
                }

                val backButton =
                    Button(this@MyPropertiesActivity).apply {
                        text = getString(R.string.back_to_dashboard)

                        setOnClickListener {
                            finish()
                        }
                    }

                propertiesContainer.addView(backButton)

            } catch (e: Exception) {

                Toast.makeText(
                    this@MyPropertiesActivity,
                    "Unable to load properties: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}