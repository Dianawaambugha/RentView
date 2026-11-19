package com.example.rentview.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.PropertyEditRequestEntity
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class EditPropertyActivity : AppCompatActivity() {

    private lateinit var database: RentViewDatabase

    private var propertyId: Long = -1L
    private var ownerId: Long = -1L

    private lateinit var titleInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var rentInput: EditText
    private lateinit var bedroomsInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_property)

        database = RentViewDatabase.getInstance(this)

        propertyId = intent.getLongExtra("PROPERTY_ID", -1L)
        ownerId = intent.getLongExtra("OWNER_ID", -1L)

        titleInput = findViewById(R.id.editPropertyTitle)
        locationInput = findViewById(R.id.editPropertyLocation)
        rentInput = findViewById(R.id.editPropertyRent)
        bedroomsInput = findViewById(R.id.editPropertyBedrooms)
        descriptionInput = findViewById(R.id.editPropertyDescription)
        submitButton = findViewById(R.id.btnSubmitPropertyEdit)

        if (propertyId == -1L || ownerId == -1L) {
            Toast.makeText(
                this,
                "Property or owner information is missing.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        loadPropertyDetails()

        submitButton.setOnClickListener {
            submitPropertyChanges()
        }
    }

    private fun loadPropertyDetails() {
        lifecycleScope.launch {

            val property = withContext(Dispatchers.IO) {
                database.propertyDao().getById(propertyId)
            }

            if (property == null || property.ownerId != ownerId) {
                Toast.makeText(
                    this@EditPropertyActivity,
                    "Property not found in your listings.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
                return@launch
            }

            titleInput.setText(property.title)
            locationInput.setText(property.location)
            rentInput.setText(property.rent.toString())
            bedroomsInput.setText(property.bedrooms.toString())
            descriptionInput.setText(property.description)
        }
    }

    private fun submitPropertyChanges() {

        val title = titleInput.text.toString().trim()
        val location = locationInput.text.toString().trim()
        val rentText = rentInput.text.toString().trim()
        val bedroomsText = bedroomsInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()

        if (
            title.isBlank() ||
            location.isBlank() ||
            rentText.isBlank() ||
            bedroomsText.isBlank() ||
            description.isBlank()
        ) {
            Toast.makeText(
                this,
                "Please fill in all fields.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rent = rentText.toDoubleOrNull()
        val bedrooms = bedroomsText.toIntOrNull()

        if (rent == null || rent <= 0) {
            rentInput.error = "Enter a valid rent greater than zero."
            return
        }

        if (bedrooms == null || bedrooms < 0) {
            bedroomsInput.error = "Enter a valid number of bedrooms."
            return
        }

        submitButton.isEnabled = false

        lifecycleScope.launch {

            val editRequestId = withContext(Dispatchers.IO) {

                val property = database.propertyDao().getById(propertyId)

                if (
                    property == null ||
                    property.ownerId != ownerId ||
                    property.propertyStatus != "APPROVED"
                ) {
                    return@withContext -1L
                }

                val existingRequests =
                    database.propertyEditRequestDao().getByProperty(propertyId)

                if (existingRequests.any { it.editStatus == "PENDING" }) {
                    return@withContext -1L
                }

                val request = PropertyEditRequestEntity(
                    propertyId = propertyId,
                    proposedTitle = title,
                    proposedLocation = location,
                    proposedRent = rent,
                    proposedBedrooms = bedrooms,
                    proposedDescription = description,
                    editStatus = "PENDING",
                    createdAt = currentTimestamp()
                )

                database.propertyEditRequestDao().insert(request)
            }

            submitButton.isEnabled = true

            if (editRequestId > 0) {
                Toast.makeText(
                    this@EditPropertyActivity,
                    "Changes submitted for admin approval.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            } else {
                Toast.makeText(
                    this@EditPropertyActivity,
                    "Could not submit changes. Check that the property is approved and has no pending edit request.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun currentTimestamp(): String {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.US
        )
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }
}