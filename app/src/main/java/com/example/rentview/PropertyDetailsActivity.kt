package com.example.rentview

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.rentview.ui.Formatters
import com.example.rentview.ui.ImageLoader
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
        val photoScroll = findViewById<View>(R.id.scrollDetailPhotos)
        val photoStrip = findViewById<LinearLayout>(R.id.layoutDetailPhotos)
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

                        showPhotos(photoStrip, photoScroll, state.photoUris)
                        photos.text = when (state.photoUris.size) {
                            0 -> "No photos uploaded"
                            1 -> "1 photo"
                            else -> "${state.photoUris.size} photos, swipe to see them all"
                        }

                        detailsLayout.visibility = View.VISIBLE
                    } else {
                        detailsLayout.visibility = View.GONE
                    }
                }
            }
        }
    }

    // Fills the sideways-scrolling strip with one picture per photo.
    private fun showPhotos(strip: LinearLayout, scroll: View, uris: List<String>) {
        strip.removeAllViews()
        scroll.visibility = if (uris.isEmpty()) View.GONE else View.VISIBLE

        val density = resources.displayMetrics.density
        val width = (260 * density).toInt()
        val height = (190 * density).toInt()
        val gap = (8 * density).toInt()

        for (uri in uris) {
            val image = ImageView(this)
            image.layoutParams = LinearLayout.LayoutParams(width, height).apply {
                marginEnd = gap
            }
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            image.setBackgroundColor(Color.parseColor("#DCE5EC"))
            image.contentDescription = "Property photo"
            strip.addView(image)

            ImageLoader.load(image, uri, 900)
        }
    }
}
