package com.example.rentview

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
import java.util.Locale

class MyPropertiesActivity : AppCompatActivity() {
    private lateinit var databaseHelper: UserDatabaseHelper
    private var ownerId: Long = -1L
    private lateinit var propertiesContainer: LinearLayout

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

        databaseHelper = UserDatabaseHelper(this)

        val scrollView = ScrollView(this)

        propertiesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Add the properties container without explicit layout parameters.
        scrollView.addView(propertiesContainer)

        setContentView(scrollView)

        displayProperties()
    }

    override fun onResume() {
        super.onResume()

        if (
            ::databaseHelper.isInitialized &&
            ::propertiesContainer.isInitialized
        ) {
            displayProperties()
        }
    }

    private fun displayProperties() {

        propertiesContainer.removeAllViews()

        val heading = TextView(this).apply {
            text = getString(R.string.my_properties_title)
            textSize = 26f
            setPadding(0, 0, 0, 24)
        }

        propertiesContainer.addView(heading)

        val cursor = databaseHelper.getOwnerProperties(ownerId)
        var propertyCount = 0

        cursor.use { properties ->

            while (properties.moveToNext()) {

                propertyCount++

                val propertyId = properties.getLong(0)
                val title = properties.getString(2)
                val location = properties.getString(3)
                val rent = properties.getDouble(4)
                val bedrooms = properties.getInt(5)
                val description = properties.getString(6)
                val status = properties.getString(7)

                val propertyCard = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 20, 20, 20)
                    elevation = 4f
                    setBackgroundColor(0xFFF2F2F2.toInt())
                }

                val formattedRent = String.format(
                    Locale.getDefault(),
                    "%,.2f",
                    rent
                )

                val propertyDetails = TextView(this).apply {
                    text = getString(
                        R.string.property_card_details,
                        title,
                        location,
                        formattedRent,
                        bedrooms,
                        description,
                        status
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

                // Only approved properties can be edited.
                if (status == UserDatabaseHelper.STATUS_APPROVED) {

                    val editButton = Button(this).apply {
                        text = getString(R.string.edit_property)
                    }

                    editButton.setOnClickListener {

                        val pendingEditId =
                            databaseHelper.getPendingPropertyEditId(propertyId)

                        if (pendingEditId != null) {

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
                                propertyId
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

        if (propertyCount == 0) {

            val emptyMessage = TextView(this).apply {
                text = getString(R.string.my_properties_empty)
                textSize = 18f
                gravity = Gravity.CENTER
            }

            propertiesContainer.addView(emptyMessage)
        }

        val backButton = Button(this).apply {
            text = getString(R.string.back_to_dashboard)

            setOnClickListener {
                finish()
            }
        }

        propertiesContainer.addView(backButton)
    }

    override fun onDestroy() {

        if (::databaseHelper.isInitialized) {
            databaseHelper.close()
        }

        super.onDestroy()
    }
}
