package com.example.rentview.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PropertyApprovalsActivity : AppCompatActivity() {

    companion object {
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_APPROVED = "APPROVED"
        private const val STATUS_REJECTED = "REJECTED"
    }

    private lateinit var database: RentViewDatabase
    private lateinit var propertyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = RentViewDatabase.getInstance(applicationContext)

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
                val intent = Intent(
                    this@PropertyApprovalsActivity,
                    AdminDashboardActivity::class.java
                )
                startActivity(intent)
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

        loadPendingProperties()
    }

    override fun onResume() {
        super.onResume()

        if (::propertyContainer.isInitialized) {
            loadPendingProperties()
        }
    }

    private fun loadPendingProperties() {

        propertyContainer.removeAllViews()

        lifecycleScope.launch {

            try {

                val pendingProperties = withContext(Dispatchers.IO) {

                    val properties = database.propertyDao()
                        .getByStatus(STATUS_PENDING)

                    properties.map { property ->

                        val owner = database.userDao()
                            .getById(property.ownerId)

                        property to owner
                    }
                }

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

                pendingProperties.forEach { (property, owner) ->

                    addPropertyCard(
                        property.propertyId,
                        property.title,
                        property.location,
                        property.rent,
                        property.bedrooms,
                        property.description,
                        owner?.fullName ?: "Unknown owner",
                        owner?.email ?: "Unavailable"
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
        propertyId: Long,
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

                        val updated = withContext(Dispatchers.IO) {

                            val property = database.propertyDao()
                                .getById(propertyId)

                            if (property == null ||
                                property.propertyStatus != STATUS_PENDING
                            ) {
                                false
                            } else {

                                database.propertyDao().updateStatus(
                                    propertyId,
                                    status
                                )

                                database.propertyDao()
                                    .getById(propertyId)
                                    ?.propertyStatus == status
                            }
                        }

                        if (updated) {

                            Toast.makeText(
                                this@PropertyApprovalsActivity,
                                "Property ${action}d successfully.",
                                Toast.LENGTH_SHORT
                            ).show()

                            loadPendingProperties()

                        } else {

                            Toast.makeText(
                                this@PropertyApprovalsActivity,
                                "Could not update the property status.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

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