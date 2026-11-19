package com.example.rentview.ui.activities

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
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PropertyEditRequestsActivity : AppCompatActivity() {

    companion object {
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_APPROVED = "APPROVED"
        private const val STATUS_REJECTED = "REJECTED"
    }

    private lateinit var database: RentViewDatabase
    private lateinit var requestsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = RentViewDatabase.getInstance(applicationContext)

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

    override fun onResume() {
        super.onResume()

        if (::requestsContainer.isInitialized) {
            displayEditRequests()
        }
    }

    private fun displayEditRequests() {

        requestsContainer.removeAllViews()

        lifecycleScope.launch {

            try {

                val requests = withContext(Dispatchers.IO) {

                    val pendingRequests = database.propertyEditRequestDao()
                        .getByStatus(STATUS_PENDING)

                    pendingRequests.map { request ->

                        val property = database.propertyDao()
                            .getById(request.propertyId)

                        val owner = property?.let {
                            database.userDao().getById(it.ownerId)
                        }

                        Triple(request, property, owner)
                    }
                }

                if (requests.isEmpty()) {

                    val emptyMessage = TextView(
                        this@PropertyEditRequestsActivity
                    ).apply {
                        text = "No pending property edit requests."
                        textSize = 17f
                        gravity = Gravity.CENTER
                        setPadding(0, 30, 0, 30)
                    }

                    requestsContainer.addView(emptyMessage)
                    return@launch
                }

                requests.forEach { (request, property, owner) ->

                    if (property == null) {
                        return@forEach
                    }

                    val editId = request.editId

                    val ownerName = owner?.fullName ?: "Unknown owner"
                    val ownerEmail = owner?.email ?: "Unavailable"

                    val card = LinearLayout(
                        this@PropertyEditRequestsActivity
                    ).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(20, 20, 20, 20)
                        setBackgroundColor(0xFFF1F1F1.toInt())
                    }

                    val details = TextView(
                        this@PropertyEditRequestsActivity
                    ).apply {

                        text = """
                            PROPERTY EDIT REQUEST #$editId

                            Property ID: ${request.propertyId}

                            OWNER INFORMATION

                            Owner Name: $ownerName
                            Owner Email: $ownerEmail

                            CURRENT PROPERTY INFORMATION

                            Title: ${property.title}
                            Location: ${property.location}
                            Monthly Rent: KES ${String.format(Locale.getDefault(), "%,.2f", property.rent)}
                            Bedrooms: ${property.bedrooms}
                            Description: ${property.description}

                            PROPOSED CHANGES

                            Title: ${request.proposedTitle}
                            Location: ${request.proposedLocation}
                            Monthly Rent: KES ${String.format(Locale.getDefault(), "%,.2f", request.proposedRent)}
                            Bedrooms: ${request.proposedBedrooms}
                            Description: ${request.proposedDescription}

                            Status: PENDING
                        """.trimIndent()

                        textSize = 15f
                        setTextColor(0xFF222222.toInt())
                    }

                    card.addView(details)

                    val buttonLayout = LinearLayout(
                        this@PropertyEditRequestsActivity
                    ).apply {
                        orientation = LinearLayout.HORIZONTAL
                    }

                    val approveButton = Button(
                        this@PropertyEditRequestsActivity
                    ).apply {
                        text = "Approve"
                    }

                    val rejectButton = Button(
                        this@PropertyEditRequestsActivity
                    ).apply {
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

            } catch (e: Exception) {

                Toast.makeText(
                    this@PropertyEditRequestsActivity,
                    "Unable to load edit requests: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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

                lifecycleScope.launch {

                    try {

                        val success = withContext(Dispatchers.IO) {

                            val editDao = database.propertyEditRequestDao()
                            val propertyDao = database.propertyDao()

                            val request = editDao.getById(editId)

                            if (request == null ||
                                request.editStatus != STATUS_PENDING
                            ) {
                                false
                            } else {

                                if (approve) {

                                    val property = propertyDao.getById(
                                        request.propertyId
                                    )

                                    if (property == null) {
                                        false
                                    } else {

                                        val updatedProperty = property.copy(
                                            title = request.proposedTitle,
                                            location = request.proposedLocation,
                                            rent = request.proposedRent,
                                            bedrooms = request.proposedBedrooms,
                                            description = request.proposedDescription
                                        )

                                        propertyDao.update(updatedProperty)

                                        editDao.updateStatus(
                                            editId,
                                            STATUS_APPROVED
                                        )

                                        true
                                    }

                                } else {

                                    editDao.updateStatus(
                                        editId,
                                        STATUS_REJECTED
                                    )

                                    true
                                }
                            }
                        }

                        if (success) {

                            Toast.makeText(
                                this@PropertyEditRequestsActivity,
                                if (approve)
                                    "Property changes approved."
                                else
                                    "Property changes rejected.",
                                Toast.LENGTH_SHORT
                            ).show()

                            displayEditRequests()

                        } else {

                            Toast.makeText(
                                this@PropertyEditRequestsActivity,
                                "Unable to process this request.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    } catch (e: Exception) {

                        Toast.makeText(
                            this@PropertyEditRequestsActivity,
                            "Request processing failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}