package com.example.rentview

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PropertyEditRequestsActivity : AppCompatActivity() {

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

        displayEditRequests()
    }

    private fun displayEditRequests() {

        requestsContainer.removeAllViews()

        databaseHelper.getPendingPropertyEdits().use { cursor ->

            if (cursor.count == 0) {

                val emptyMessage = TextView(this).apply {
                    text = "No pending property edit requests."
                    textSize = 17f
                    gravity = Gravity.CENTER
                    setPadding(0, 30, 0, 30)
                }

                requestsContainer.addView(emptyMessage)
                return
            }

            val editIdIndex =
                cursor.getColumnIndexOrThrow("edit_id")

            val propertyIdIndex =
                cursor.getColumnIndexOrThrow("property_id")

            val proposedTitleIndex =
                cursor.getColumnIndexOrThrow("proposed_title")

            val proposedLocationIndex =
                cursor.getColumnIndexOrThrow("proposed_location")

            val proposedRentIndex =
                cursor.getColumnIndexOrThrow("proposed_rent")

            val proposedBedroomsIndex =
                cursor.getColumnIndexOrThrow("proposed_bedrooms")

            val proposedDescriptionIndex =
                cursor.getColumnIndexOrThrow("proposed_description")

            val currentTitleIndex =
                cursor.getColumnIndexOrThrow("current_title")

            val currentLocationIndex =
                cursor.getColumnIndexOrThrow("current_location")

            val currentRentIndex =
                cursor.getColumnIndexOrThrow("current_rent")

            val currentBedroomsIndex =
                cursor.getColumnIndexOrThrow("current_bedrooms")

            val currentDescriptionIndex =
                cursor.getColumnIndexOrThrow("current_description")

            val ownerNameIndex =
                cursor.getColumnIndexOrThrow("owner_name")

            val ownerEmailIndex =
                cursor.getColumnIndexOrThrow("owner_email")

            while (cursor.moveToNext()) {

                val editId = cursor.getLong(editIdIndex)

                val propertyId =
                    cursor.getLong(propertyIdIndex)

                val proposedTitle =
                    cursor.getString(proposedTitleIndex)

                val proposedLocation =
                    cursor.getString(proposedLocationIndex)

                val proposedRent =
                    cursor.getDouble(proposedRentIndex)

                val proposedBedrooms =
                    cursor.getInt(proposedBedroomsIndex)

                val proposedDescription =
                    cursor.getString(proposedDescriptionIndex)

                val currentTitle =
                    cursor.getString(currentTitleIndex)

                val currentLocation =
                    cursor.getString(currentLocationIndex)

                val currentRent =
                    cursor.getDouble(currentRentIndex)

                val currentBedrooms =
                    cursor.getInt(currentBedroomsIndex)

                val currentDescription =
                    cursor.getString(currentDescriptionIndex)

                val ownerName =
                    cursor.getString(ownerNameIndex)

                val ownerEmail =
                    cursor.getString(ownerEmailIndex)

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 20, 20, 20)
                    setBackgroundColor(0xFFF1F1F1.toInt())
                }

                val details = TextView(this).apply {

                    text = """
                        PROPERTY EDIT REQUEST #$editId

                        Property ID: $propertyId

                        OWNER INFORMATION

                        Owner Name: $ownerName
                        Owner Email: $ownerEmail

                        CURRENT PROPERTY INFORMATION

                        Title: $currentTitle
                        Location: $currentLocation
                        Monthly Rent: KES ${"%,.2f".format(currentRent)}
                        Bedrooms: $currentBedrooms
                        Description: $currentDescription

                        PROPOSED CHANGES

                        Title: $proposedTitle
                        Location: $proposedLocation
                        Monthly Rent: KES ${"%,.2f".format(proposedRent)}
                        Bedrooms: $proposedBedrooms
                        Description: $proposedDescription

                        Status: PENDING
                    """.trimIndent()

                    textSize = 15f
                    setTextColor(0xFF222222.toInt())
                }

                card.addView(details)

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
                    confirmDecision(editId, true)
                }

                rejectButton.setOnClickListener {
                    confirmDecision(editId, false)
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
        }
    }

    private fun confirmDecision(
        editId: Long,
        approve: Boolean
    ) {

        val action = if (approve) "approve" else "reject"

        AlertDialog.Builder(this)
            .setTitle("Confirm Decision")
            .setMessage("Are you sure you want to $action this property edit?")
            .setPositiveButton("Yes") { _, _ ->

                val success = if (approve) {
                    databaseHelper.approvePropertyEdit(editId)
                } else {
                    databaseHelper.rejectPropertyEdit(editId)
                }

                if (success) {

                    Toast.makeText(
                        this,
                        if (approve)
                            "Property changes approved."
                        else
                            "Property changes rejected.",
                        Toast.LENGTH_SHORT
                    ).show()

                    displayEditRequests()

                } else {

                    Toast.makeText(
                        this,
                        "Unable to process this request.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}