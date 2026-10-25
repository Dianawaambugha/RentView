package com.example.rentview

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditPropertyActivity : AppCompatActivity() {

    private lateinit var databaseHelper: UserDatabaseHelper

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

        databaseHelper = UserDatabaseHelper(this)

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
        val cursor = databaseHelper.getOwnerProperties(ownerId)

        cursor.use {
            while (it.moveToNext()) {
                val currentPropertyId = it.getLong(
                    it.getColumnIndexOrThrow(
                        UserDatabaseHelper.COL_PROPERTY_ID
                    )
                )

                if (currentPropertyId == propertyId) {
                    titleInput.setText(
                        it.getString(
                            it.getColumnIndexOrThrow(
                                UserDatabaseHelper.COL_PROPERTY_TITLE
                            )
                        )
                    )

                    locationInput.setText(
                        it.getString(
                            it.getColumnIndexOrThrow(
                                UserDatabaseHelper.COL_PROPERTY_LOCATION
                            )
                        )
                    )

                    rentInput.setText(
                        it.getDouble(
                            it.getColumnIndexOrThrow(
                                UserDatabaseHelper.COL_PROPERTY_RENT
                            )
                        ).toString()
                    )

                    bedroomsInput.setText(
                        it.getInt(
                            it.getColumnIndexOrThrow(
                                UserDatabaseHelper.COL_PROPERTY_BEDROOMS
                            )
                        ).toString()
                    )

                    descriptionInput.setText(
                        it.getString(
                            it.getColumnIndexOrThrow(
                                UserDatabaseHelper.COL_PROPERTY_DESCRIPTION
                            )
                        )
                    )

                    return
                }
            }
        }

        Toast.makeText(
            this,
            "Property not found in your listings.",
            Toast.LENGTH_LONG
        ).show()

        finish()
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

        val editRequestId = databaseHelper.submitPropertyEdit(
            ownerId = ownerId,
            propertyId = propertyId,
            title = title,
            location = location,
            rent = rent,
            bedrooms = bedrooms,
            description = description,
            photoUris = emptyList()
        )

        if (editRequestId != -1L) {
            Toast.makeText(
                this,
                "Changes submitted for admin approval.",
                Toast.LENGTH_LONG
            ).show()

            finish()
        } else {
            Toast.makeText(
                this,
                "Could not submit changes. Check that the property is approved and has no pending edit request.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }
}