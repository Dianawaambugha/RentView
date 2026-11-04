package com.example.rentview

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AllPropertiesActivity : AppCompatActivity() {

    private lateinit var databaseHelper: UserDatabaseHelper
    private lateinit var propertiesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = UserDatabaseHelper.getInstance(this)

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

        val cursor = databaseHelper.getAllProperties()

        cursor.use {

            if (it.count == 0) {
                val emptyMessage = TextView(this).apply {
                    text = "No properties have been listed yet."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(0, 30, 0, 30)
                }

                propertiesContainer.addView(emptyMessage)
                return
            }

            val propertyIdIndex =
                it.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_PROPERTY_ID
                )

            val titleIndex =
                it.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_PROPERTY_TITLE
                )

            val locationIndex =
                it.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_PROPERTY_LOCATION
                )

            val rentIndex =
                it.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_PROPERTY_RENT
                )

            val bedroomsIndex =
                it.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_PROPERTY_BEDROOMS
                )

            val statusIndex =
                it.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_PROPERTY_STATUS
                )

            val ownerNameIndex =
                it.getColumnIndexOrThrow("owner_name")

            val ownerEmailIndex =
                it.getColumnIndexOrThrow("owner_email")

            val createdAtIndex =
                it.getColumnIndexOrThrow(
                    UserDatabaseHelper.COL_CREATED_AT
                )

            val viewingRequestsIndex =
                it.getColumnIndexOrThrow("viewing_requests")

            while (it.moveToNext()) {

                val propertyId = it.getLong(propertyIdIndex)

                val propertyTitle =
                    it.getString(titleIndex) ?: "Untitled Property"

                val location =
                    it.getString(locationIndex)
                        ?: "Location not provided"

                val rent = it.getDouble(rentIndex)

                val bedrooms = it.getInt(bedroomsIndex)

                val status =
                    it.getString(statusIndex) ?: "UNKNOWN"

                val ownerName =
                    it.getString(ownerNameIndex) ?: "Unknown Owner"

                val ownerEmail =
                    it.getString(ownerEmailIndex)
                        ?: "No email available"

                val createdAt =
                    it.getString(createdAtIndex) ?: "Unknown"

                val viewingRequests =
                    it.getInt(viewingRequestsIndex)

                val propertyCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 20, 20, 20)
                    setBackgroundColor(0xFFF1F1F1.toInt())
                }

                val details = TextView(this).apply {

                    text = """
                    PROPERTY DETAILS
                    
                    Property ID: $propertyId
                    Title: $propertyTitle
                    Location: $location
                    Monthly Rent: KES ${"%,.2f".format(rent)}
                    Bedrooms: $bedrooms
                    Status: $status
                    
                    OWNER INFORMATION
                    
                    Owner Name: $ownerName
                    Owner Email: $ownerEmail
                    
                    LISTING INFORMATION
                    
                    Date Added: $createdAt
                    Viewing Requests: $viewingRequests
                """.trimIndent()

                    textSize = 16f
                    setTextColor(0xFF222222.toInt())
                }

                propertyCard.addView(details)

                val cardParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 20
                }

                propertiesContainer.addView(
                    propertyCard,
                    cardParams
                )
            }
        }
    }
}
