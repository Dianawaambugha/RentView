package com.example.rentview

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.rentview.ui.Formatters
import com.example.rentview.viewmodel.PropertyDetailsViewModel
import kotlinx.coroutines.launch

// Tenant screen: full details of one property, with a button to book a viewing.
class PropertyDetailsActivity : AppCompatActivity() {

    private val viewModel: PropertyDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_property_details)

        // Data passed from the property list screen.
        val propertyId = intent.getLongExtra("PROPERTY_ID", -1L)
        val userId = intent.getLongExtra("USER_ID", -1L)

        val progress = findViewById<ProgressBar>(R.id.progressDetails)
        val errorText = findViewById<TextView>(R.id.tvDetailsError)
        val detailsLayout = findViewById<LinearLayout>(R.id.layoutDetails)
        val title = findViewById<TextView>(R.id.tvDetailTitle)
        val location = findViewById<TextView>(R.id.tvDetailLocation)
        val rent = findViewById<TextView>(R.id.tvDetailRent)
        val bedrooms = findViewById<TextView>(R.id.tvDetailBedrooms)
        val description = findViewById<TextView>(R.id.tvDetailDescription)
        val photos = findViewById<TextView>(R.id.tvDetailPhotos)
        val scheduleButton = findViewById<Button>(R.id.btnScheduleViewing)
        val backButton = findViewById<Button>(R.id.btnDetailsBack)

        backButton.setOnClickListener { finish() }

        scheduleButton.setOnClickListener {
            val scheduleIntent = Intent(this, ScheduleViewingActivity::class.java)
            scheduleIntent.putExtra("PROPERTY_ID", propertyId)
            scheduleIntent.putExtra("USER_ID", userId)
            startActivity(scheduleIntent)
        }

        viewModel.load(propertyId)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->

                    progress.visibility =
                        if (state.isLoading) View.VISIBLE else View.GONE

                    errorText.visibility =
                        if (state.error != null) View.VISIBLE else View.GONE
                    errorText.text = state.error ?: ""

                    val property = state.property

                    if (property != null) {
                        title.text = property.title
                        location.text = property.location
                        rent.text = Formatters.rent(property.rent)
                        bedrooms.text = Formatters.bedrooms(property.bedrooms)
                        description.text = property.description
                        photos.text = when (state.photoCount) {
                            0 -> "No photos uploaded"
                            1 -> "1 photo uploaded"
                            else -> "${state.photoCount} photos uploaded"
                        }
                        detailsLayout.visibility = View.VISIBLE
                    } else {
                        detailsLayout.visibility = View.GONE
                    }
                }
            }
        }
    }
}
