package com.example.rentview.ui.activities

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewingRequestsActivity : AppCompatActivity() {

    private lateinit var requestsContainer: LinearLayout

    private val database by lazy {
        RentViewDatabase.getInstance(applicationContext)
    }

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

    private fun displayViewingRequests() {

        requestsContainer.removeAllViews()

        lifecycleScope.launch {

            val requests = withContext(Dispatchers.IO) {

                val propertyDao = database.propertyDao()
                val viewingDao = database.viewingDao()
                val userDao = database.userDao()

                val allProperties = propertyDao.getAll()

                val requestDetails = mutableListOf<String>()

                for (property in allProperties) {

                    val viewings =
                        viewingDao.getByProperty(property.propertyId)

                    val owner =
                        userDao.getById(property.ownerId)

                    for (viewing in viewings) {

                        val tenant =
                            userDao.getById(viewing.tenantId)

                        val propertyTitle =
                            property.title.ifBlank {
                                "Unknown Property"
                            }

                        val propertyLocation =
                            property.location.ifBlank {
                                "Location unavailable"
                            }

                        val ownerName =
                            owner?.fullName ?: "Unknown Owner"

                        val ownerPhone =
                            owner?.phone ?: "Not provided"

                        val ownerEmail =
                            owner?.email ?: "Not provided"

                        val tenantName =
                            tenant?.fullName ?: "Unknown Tenant"

                        val tenantPhone =
                            tenant?.phone ?: "Not provided"

                        val tenantEmail =
                            tenant?.email ?: "Not provided"

                        val viewingDate =
                            viewing.viewingDate.ifBlank {
                                "Not specified"
                            }

                        val viewingSlot =
                            viewing.viewingSlot.ifBlank {
                                "Not specified"
                            }

                        val details = """
                            PROPERTY INFORMATION

                            Property: $propertyTitle
                            Location: $propertyLocation
                            Monthly Rent: KES ${"%,.2f".format(property.rent)}

                            OWNER INFORMATION

                            Owner Name: $ownerName
                            Owner Phone: $ownerPhone
                            Owner Email: $ownerEmail

                            TENANT INFORMATION

                            Tenant Name: $tenantName
                            Tenant Phone: $tenantPhone
                            Tenant Email: $tenantEmail

                            VIEWING APPOINTMENT

                            Date: $viewingDate
                            Time Slot: $viewingSlot
                        """.trimIndent()

                        requestDetails.add(details)
                    }
                }

                requestDetails
            }

            requestsContainer.removeAllViews()

            if (requests.isEmpty()) {

                val emptyMessage = TextView(this@ViewingRequestsActivity).apply {
                    text = "No viewing requests have been made yet."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(0, 30, 0, 30)
                }

                requestsContainer.addView(emptyMessage)
                return@launch
            }

            for (request in requests) {

                val requestCard =
                    LinearLayout(this@ViewingRequestsActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(20, 20, 20, 20)
                        setBackgroundColor(0xFFF1F1F1.toInt())
                    }

                val details =
                    TextView(this@ViewingRequestsActivity).apply {
                        text = request
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
        }
    }
}