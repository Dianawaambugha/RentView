package com.example.rentview.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.PropertyEntity
import com.example.rentview.data.PropertyPhotoEntity
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AddPropertyActivity : AppCompatActivity() {

    private val selectedPhotos = mutableListOf<Uri>()
    private lateinit var database: RentViewDatabase

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        selectedPhotos.clear()

        for (uri in uris) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                continue
            }

            selectedPhotos.add(uri)
        }

        val photoText = findViewById<TextView>(R.id.tvSelectedPhotos)
        photoText.text = if (selectedPhotos.isEmpty()) {
            "No photos selected"
        } else {
            "${selectedPhotos.size} photo(s) selected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_property)

        val root = findViewById<View>(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())

            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                maxOf(bars.bottom, keyboard.bottom)
            )

            insets
        }

        database = RentViewDatabase.getInstance(this)

        val ownerId = intent.getLongExtra("USER_ID", -1L)

        val title = findViewById<EditText>(R.id.etPropertyTitle)
        val location = findViewById<EditText>(R.id.etPropertyLocation)
        val rent = findViewById<EditText>(R.id.etPropertyRent)
        val bedrooms = findViewById<EditText>(R.id.etPropertyBedrooms)
        val description = findViewById<EditText>(R.id.etPropertyDescription)
        val choosePhotosButton = findViewById<Button>(R.id.btnChoosePhotos)
        val saveButton = findViewById<Button>(R.id.btnSaveProperty)
        val cancelButton = findViewById<Button>(R.id.btnCancelProperty)

        choosePhotosButton.setOnClickListener {
            photoPicker.launch(arrayOf("image/*"))
        }

        cancelButton.setOnClickListener {
            finish()
        }

        saveButton.setOnClickListener {
            val titleValue = title.text.toString().trim()
            val locationValue = location.text.toString().trim()
            val rentValue = rent.text.toString().trim().toDoubleOrNull()
            val bedroomsValue = bedrooms.text.toString().trim().toIntOrNull()
            val descriptionValue = description.text.toString().trim()

            when {
                ownerId == -1L -> {
                    Toast.makeText(
                        this,
                        "Could not identify your account. Please log in again.",
                        Toast.LENGTH_LONG
                    ).show()
                }

                titleValue.isEmpty() -> title.error = "Enter the property title"

                locationValue.isEmpty() -> location.error = "Enter the location"

                rentValue == null || rentValue <= 0 ->
                    rent.error = "Enter a valid monthly rent"

                bedroomsValue == null || bedroomsValue < 0 ->
                    bedrooms.error = "Enter the number of bedrooms"

                descriptionValue.isEmpty() ->
                    description.error = "Enter a description"

                selectedPhotos.isEmpty() -> {
                    Toast.makeText(
                        this,
                        "Please choose at least one photo.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    saveButton.isEnabled = false

                    lifecycleScope.launch {
                        val propertyId = withContext(Dispatchers.IO) {
                            val owner = database.userDao().getById(ownerId)
                            if (owner == null ||
                                owner.role != "Property Owner" ||
                                owner.approvalStatus != "APPROVED"
                            ) {
                                return@withContext -1L
                            }

                            val property = PropertyEntity(
                                ownerId = ownerId,
                                title = titleValue,
                                location = locationValue,
                                rent = rentValue,
                                bedrooms = bedroomsValue,
                                description = descriptionValue,
                                propertyStatus = "PENDING",
                                createdAt = currentTimestamp()
                            )

                            val newPropertyId =
                                database.propertyDao().insert(property)

                            if (newPropertyId > 0) {
                                val photos = selectedPhotos.map { uri ->
                                    PropertyPhotoEntity(
                                        propertyId = newPropertyId,
                                        photoUri = uri.toString()
                                    )
                                }

                                database.propertyPhotoDao().insertAll(photos)
                            }

                            newPropertyId
                        }

                        saveButton.isEnabled = true

                        if (propertyId > 0) {
                            Toast.makeText(
                                this@AddPropertyActivity,
                                "Property submitted. It will appear to tenants once the administrator approves it.",
                                Toast.LENGTH_LONG
                            ).show()

                            finish()
                        } else {
                            Toast.makeText(
                                this@AddPropertyActivity,
                                "Could not save the property. Make sure your owner account is approved.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
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