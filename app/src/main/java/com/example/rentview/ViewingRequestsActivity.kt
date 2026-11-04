package com.example.rentview

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ViewingRequestsActivity : AppCompatActivity() {
    private lateinit var databaseHelper: UserDatabaseHelper
    private lateinit var requestsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = UserDatabaseHelper.getInstance(this)

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

        val cursor = databaseHelper.getAllViewingRequests()

        cursor.use {

            if (it.count == 0) {
                val emptyMessage = TextView(this).apply {
                    text = "No viewing requests have been made yet."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(0, 30, 0, 30)
                }

                requestsContainer.addView(emptyMessage)
                return
            }

            val propertyTitleIndex =
                it.getColumnIndexOrThrow("property_title")

            val propertyLocationIndex =
                it.getColumnIndexOrThrow("property_location")

            val propertyRentIndex =
                it.getColumnIndexOrThrow("property_rent")

            val ownerNameIndex =
                it.getColumnIndexOrThrow("owner_name")

            val ownerPhoneIndex =
                it.getColumnIndexOrThrow("owner_phone")

            val ownerEmailIndex =
                it.getColumnIndexOrThrow("owner_email")

            val tenantNameIndex =
                it.getColumnIndexOrThrow("tenant_name")

            val tenantPhoneIndex =
                it.getColumnIndexOrThrow("tenant_phone")

            val tenantEmailIndex =
                it.getColumnIndexOrThrow("tenant_email")

            val viewingDateIndex =
                it.getColumnIndexOrThrow("viewing_date")

            val viewingSlotIndex =
                it.getColumnIndexOrThrow("viewing_slot")

            while (it.moveToNext()) {

                val propertyTitle =
                    it.getString(propertyTitleIndex) ?: "Unknown Property"

                val propertyLocation =
                    it.getString(propertyLocationIndex)
                        ?: "Location unavailable"

                val propertyRent =
                    it.getDouble(propertyRentIndex)

                val ownerName =
                    it.getString(ownerNameIndex) ?: "Unknown Owner"

                val ownerPhone =
                    it.getString(ownerPhoneIndex) ?: "Not provided"

                val ownerEmail =
                    it.getString(ownerEmailIndex) ?: "Not provided"

                val tenantName =
                    it.getString(tenantNameIndex) ?: "Unknown Tenant"

                val tenantPhone =
                    it.getString(tenantPhoneIndex) ?: "Not provided"

                val tenantEmail =
                    it.getString(tenantEmailIndex) ?: "Not provided"

                val viewingDate =
                    it.getString(viewingDateIndex) ?: "Not specified"

                val viewingSlot =
                    it.getString(viewingSlotIndex) ?: "Not specified"

                val requestCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 20, 20, 20)
                    setBackgroundColor(0xFFF1F1F1.toInt())
                }

                val details = TextView(this).apply {
                    text = """
                    PROPERTY INFORMATION
                    
                    Property: $propertyTitle
                    Location: $propertyLocation
                    Monthly Rent: KES ${"%,.2f".format(propertyRent)}
                    
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
