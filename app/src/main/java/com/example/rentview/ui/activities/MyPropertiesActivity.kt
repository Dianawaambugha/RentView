package com.example.rentview.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MyPropertiesActivity : AppCompatActivity() {

    companion object {
        private const val STATUS_APPROVED = "APPROVED"
        private const val STATUS_PENDING = "PENDING"
    }

    private var ownerId: Long = -1L
    private lateinit var propertiesContainer: LinearLayout
    private lateinit var database: RentViewDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ownerId = intent.getLongExtra("USER_ID", -1L)

        if (ownerId == -1L) {
            Toast.makeText(
                this,
                getString(R.string.owner_not_identified),
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        database = RentViewDatabase.getInstance(applicationContext)

        val scrollView = ScrollView(this)

        propertiesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        scrollView.addView(propertiesContainer)
        setContentView(scrollView)

        displayProperties()
    }

    override fun onResume() {
        super.onResume()

        if (::propertiesContainer.isInitialized) {
            displayProperties()
        }
    }

    private fun displayProperties() {

        lifecycleScope.launch {

            propertiesContainer.removeAllViews()

            try {

                val properties = withContext(Dispatchers.IO) {

                    val propertyList = database.propertyDao().getByOwner(ownerId)

                    propertyList.map { property ->

                        val pendingEdit = database.propertyEditRequestDao()
                            .getByProperty(property.propertyId)
                            .any { it.editStatus == STATUS_PENDING }

                        property to pendingEdit
                    }
                }

                val heading = TextView(this@MyPropertiesActivity).apply {
                    text = getString(R.string.my_properties_title)
                    textSize = 26f
                    setPadding(0, 0, 0, 24)
                }

                propertiesContainer.addView(heading)

                if (properties.isEmpty()) {

                    val emptyMessage = TextView(this@MyPropertiesActivity).apply {
                        text = getString(R.string.my_properties_empty)
                        textSize = 18f
                        gravity = Gravity.CENTER
                    }

                    propertiesContainer.addView(emptyMessage)

                } else {

                    properties.forEach { (property, hasPendingEdit) ->

                        val propertyCard = LinearLayout(this@MyPropertiesActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(20, 20, 20, 20)
                            elevation = 4f
                            setBackgroundColor(0xFFF2F2F2.toInt())
                        }

                        val formattedRent = String.format(
                            Locale.getDefault(),
                            "%,.2f",
                            property.rent
                        )

                        val propertyDetails = TextView(this@MyPropertiesActivity).apply {
                            text = getString(
                                R.string.property_card_details,
                                property.title,
                                property.location,
                                formattedRent,
                                property.bedrooms,
                                property.description,
                                property.propertyStatus
                            )

                            textSize = 16f
                        }

                        propertyCard.addView(
                            propertyDetails,
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        )

                        if (property.propertyStatus == STATUS_APPROVED) {

                            val editButton = Button(this@MyPropertiesActivity).apply {
                                text = getString(R.string.edit_property)
                            }

                            editButton.setOnClickListener {

                                if (hasPendingEdit) {

                                    Toast.makeText(
                                        this@MyPropertiesActivity,
                                        getString(R.string.property_edit_pending),
                                        Toast.LENGTH_LONG
                                    ).show()

                                } else {

                                    val editIntent = Intent(
                                        this@MyPropertiesActivity,
                                        EditPropertyActivity::class.java
                                    )

                                    editIntent.putExtra(
                                        "PROPERTY_ID",
                                        property.propertyId
                                    )

                                    editIntent.putExtra(
                                        "OWNER_ID",
                                        ownerId
                                    )

                                    startActivity(editIntent)
                                }
                            }

                            propertyCard.addView(editButton)
                        }

                        propertiesContainer.addView(
                            propertyCard,
                            LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = 24
                            }
                        )
                    }
                }

                val backButton = Button(this@MyPropertiesActivity).apply {
                    text = getString(R.string.back_to_dashboard)

                    setOnClickListener {
                        finish()
                    }
                }

                propertiesContainer.addView(backButton)

            } catch (e: Exception) {

                Toast.makeText(
                    this@MyPropertiesActivity,
                    "Unable to load properties: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}