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
import com.example.rentview.data.PropertyEntity
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AllPropertiesActivity : AppCompatActivity() {

    private lateinit var database: RentViewDatabase
    private lateinit var propertiesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = RentViewDatabase.getInstance(this)

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
        lifecycleScope.launch {

            val propertyDetails = withContext(Dispatchers.IO) {
                val properties = database.propertyDao().getAll()

                properties.map { property ->

                    val owner = database.userDao().getById(property.ownerId)

                    val viewingRequests =
                        database.viewingDao().getByProperty(property.propertyId).size

                    Triple(
                        property,
                        owner,
                        viewingRequests
                    )
                }
            }

            propertiesContainer.removeAllViews()

            if (propertyDetails.isEmpty()) {
                val emptyMessage = TextView(this@AllPropertiesActivity).apply {
                    text = "No properties have been listed yet."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(0, 30, 0, 30)
                }

                propertiesContainer.addView(emptyMessage)
                return@launch
            }

            propertyDetails.forEach { (property, owner, viewingRequests) ->
                addPropertyCard(
                    property,
                    owner?.fullName ?: "Unknown Owner",
                    owner?.email ?: "No email available",
                    viewingRequests
                )
            }
        }
    }

    private fun addPropertyCard(
        property: PropertyEntity,
        ownerName: String,
        ownerEmail: String,
        viewingRequests: Int
    ) {

        val propertyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(0xFFF1F1F1.toInt())
        }

        val rentFormatted = String.format(
            Locale.US,
            "%,.2f",
            property.rent
        )

        val details = TextView(this).apply {
            text = """
                PROPERTY DETAILS

                Property ID: ${property.propertyId}
                Title: ${property.title}
                Location: ${property.location}
                Monthly Rent: KES $rentFormatted
                Bedrooms: ${property.bedrooms}
                Status: ${property.propertyStatus}

                OWNER INFORMATION

                Owner Name: $ownerName
                Owner Email: $ownerEmail

                LISTING INFORMATION

                Date Added: ${property.createdAt}
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

        propertiesContainer.addView(propertyCard, cardParams)
    }
}