package com.example.rentview

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class PropertyApprovalsActivity : AppCompatActivity() {

    private lateinit var databaseHelper: UserDatabaseHelper
    private lateinit var propertyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = UserDatabaseHelper(this)

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

        setContentView(rootLayout)

        loadPendingProperties()
    }

    private fun loadPendingProperties() {
        propertyContainer.removeAllViews()

        val cursor = databaseHelper.getPendingProperties()

        cursor.use {
            if (!it.moveToFirst()) {
                val emptyMessage = TextView(this).apply {
                    text = "No property listings are waiting for review."
                    textSize = 17f
                    gravity = Gravity.CENTER
                    setPadding(12, 32, 12, 32)
                }

                propertyContainer.addView(emptyMessage)
                return
            }

            do {
                val propertyId = it.getLong(
                    it.getColumnIndexOrThrow("property_id")
                )

                val title = it.getString(
                    it.getColumnIndexOrThrow("title")
                )

                val location = it.getString(
                    it.getColumnIndexOrThrow("location")
                )

                val rent = it.getDouble(
                    it.getColumnIndexOrThrow("rent")
                )

                val bedrooms = it.getInt(
                    it.getColumnIndexOrThrow("bedrooms")
                )

                val description = it.getString(
                    it.getColumnIndexOrThrow("description")
                )

                val ownerName = it.getString(
                    it.getColumnIndexOrThrow("owner_name")
                )

                val ownerEmail = it.getString(
                    it.getColumnIndexOrThrow("owner_email")
                )

                addPropertyCard(
                    propertyId,
                    title,
                    location,
                    rent,
                    bedrooms,
                    description,
                    ownerName,
                    ownerEmail
                )

            } while (it.moveToNext())
        }
    }

    private fun addPropertyCard(
        propertyId: Long,
        title: String,
        location: String,
        rent: Double,
        bedrooms: Int,
        description: String,
        ownerName: String,
        ownerEmail: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(android.graphics.Color.WHITE)
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
                UserDatabaseHelper.STATUS_APPROVED
            )
        }

        rejectButton.setOnClickListener {
            confirmPropertyDecision(
                propertyId,
                title,
                UserDatabaseHelper.STATUS_REJECTED
            )
        }
    }

    private fun confirmPropertyDecision(
        propertyId: Long,
        title: String,
        status: String
    ) {
        val action = if (
            status == UserDatabaseHelper.STATUS_APPROVED
        ) "approve" else "reject"

        AlertDialog.Builder(this)
            .setTitle("Confirm Decision")
            .setMessage("Are you sure you want to $action \"$title\"?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->

                val updated = databaseHelper.updatePropertyApproval(
                    propertyId,
                    status
                )

                if (updated) {
                    Toast.makeText(
                        this,
                        "Property ${action}d successfully.",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadPendingProperties()
                } else {
                    Toast.makeText(
                        this,
                        "Could not update the property status.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        databaseHelper.close()
    }

}
