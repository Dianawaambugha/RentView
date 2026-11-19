package com.example.rentview.ui.activities

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

class ViewingRequestsActivity : AppCompatActivity() {

    private lateinit var requestsContainer: LinearLayout

    private val supabase by lazy {
        SupabaseClient.client
    }

    private data class ViewingDetails(
        val propertyTitle: String,
        val propertyLocation: String,
        val rent: Double,
        val ownerName: String,
        val ownerPhone: String,
        val ownerEmail: String,
        val tenantName: String,
        val tenantPhone: String,
        val tenantEmail: String,
        val viewingDate: String,
        val viewingSlot: String,
        val status: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "Viewing Requests"
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 20)
            setTextColor(0xFF222222.toInt())
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

        displayViewingRequests()
    }

    override fun onResume() {
        super.onResume()

        if (::requestsContainer.isInitialized) {
            displayViewingRequests()
        }
    }

    private fun displayViewingRequests() {

        requestsContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val viewings = supabase
                    .from("viewings")
                    .select()
                    .decodeList<JsonObject>()

                val requestDetails = mutableListOf<ViewingDetails>()

                for (viewing in viewings) {

                    val propertyId = viewing["property_id"]
                        ?.jsonPrimitive?.content ?: continue

                    val tenantId = viewing["tenant_id"]
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
                        ?.jsonPrimitive?.content ?: continue

                    val ownerRows = supabase
                        .from("profiles")
                        .select {
                            filter {
                                eq("id", ownerId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val tenantRows = supabase
                        .from("profiles")
                        .select {
                            filter {
                                eq("id", tenantId)
                            }
                        }
                        .decodeList<JsonObject>()

                    val owner = ownerRows.firstOrNull()
                    val tenant = tenantRows.firstOrNull()

                    requestDetails.add(
                        ViewingDetails(
                            propertyTitle = property["title"]
                                ?.jsonPrimitive?.content
                                ?: "Unknown Property",

                            propertyLocation = property["location"]
                                ?.jsonPrimitive?.content
                                ?: "Location unavailable",

                            rent = property["rent"]
                                ?.jsonPrimitive?.doubleOrNull ?: 0.0,

                            ownerName = owner?.get("full_name")
                                ?.jsonPrimitive?.content
                                ?: "Unknown Owner",

                            ownerPhone = owner?.get("phone")
                                ?.jsonPrimitive?.content
                                ?: "Not provided",

                            ownerEmail = owner?.get("email")
                                ?.jsonPrimitive?.content
                                ?: "Not provided",

                            tenantName = tenant?.get("full_name")
                                ?.jsonPrimitive?.content
                                ?: "Unknown Tenant",

                            tenantPhone = tenant?.get("phone")
                                ?.jsonPrimitive?.content
                                ?: "Not provided",

                            tenantEmail = tenant?.get("email")
                                ?.jsonPrimitive?.content
                                ?: "Not provided",

                            viewingDate = viewing["viewing_date"]
                                ?.jsonPrimitive?.content
                                ?: "Not specified",

                            viewingSlot = viewing["viewing_slot"]
                                ?.jsonPrimitive?.content
                                ?: "Not specified",

                            status = viewing["status"]
                                ?.jsonPrimitive?.content
                                ?: "UNKNOWN"
                        )
                    )
                }

                requestsContainer.removeAllViews()

                if (requestDetails.isEmpty()) {

                    requestsContainer.addView(
                        TextView(this@ViewingRequestsActivity).apply {
                            text = "No viewing requests have been made yet."
                            textSize = 16f
                            gravity = Gravity.CENTER
                            setPadding(0, 30, 0, 30)
                        }
                    )

                    return@launch
                }

                for (request in requestDetails) {

                    val requestCard = LinearLayout(
                        this@ViewingRequestsActivity
                    ).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(20, 20, 20, 20)
                        setBackgroundColor(0xFFF1F1F1.toInt())
                    }

                    val details = TextView(
                        this@ViewingRequestsActivity
                    ).apply {
                        text = """
                            PROPERTY INFORMATION

                            Property: ${request.propertyTitle}
                            Location: ${request.propertyLocation}
                            Monthly Rent: KES ${"%,.2f".format(request.rent)}

                            OWNER INFORMATION

                            Owner Name: ${request.ownerName}
                            Owner Phone: ${request.ownerPhone}
                            Owner Email: ${request.ownerEmail}

                            TENANT INFORMATION

                            Tenant Name: ${request.tenantName}
                            Tenant Phone: ${request.tenantPhone}
                            Tenant Email: ${request.tenantEmail}

                            VIEWING APPOINTMENT

                            Date: ${request.viewingDate}
                            Time Slot: ${request.viewingSlot}
                            Status: ${request.status}
                        """.trimIndent()

                        textSize = 16f
                        setTextColor(0xFF222222.toInt())
                    }

                    requestCard.addView(details)

                    val cardParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 20
                    }

                    requestsContainer.addView(requestCard, cardParams)
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@ViewingRequestsActivity,
                    "Unable to load viewing requests: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}