package com.example.rentvew

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class AddPropertyActivity : AppCompatActivity() {

    private val selectedPhotos = mutableListOf<Uri>()

    // Open the photo picker and allow multiple image selections
    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->

        selectedPhotos.clear()
        selectedPhotos.addAll(uris)

        val photoCount = selectedPhotos.size
        val photoText = findViewById<TextView>(R.id.tvSelectedPhotos)

        photoText.text = if (photoCount == 0) {
            "No photos selected"
        } else {
            "$photoCount photo(s) selected"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_property)

        val choosePhotosButton =
            findViewById<Button>(R.id.btnChoosePhotos)

        val cancelButton =
            findViewById<Button>(R.id.btnCancelProperty)

        // Let the owner choose multiple property photos
        choosePhotosButton.setOnClickListener {
            photoPicker.launch("image/*")
        }

        // Return to the previous screen
        cancelButton.setOnClickListener {
            finish()
        }
    }
}

