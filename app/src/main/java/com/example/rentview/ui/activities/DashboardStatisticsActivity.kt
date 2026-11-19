package com.example.rentview.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardStatisticsActivity : AppCompatActivity() {

    private lateinit var database: RentViewDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = RentViewDatabase.getInstance(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 24)
        }

        val title = TextView(this).apply {
            text = "Dashboard Statistics"
            textSize = 26f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }

        mainLayout.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val statisticsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            addView(statisticsLayout)
        }

        mainLayout.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val backButton = Button(this).apply {
            text = "Back to Dashboard"
            setOnClickListener {
                startActivity(
                    Intent(
                        this@DashboardStatisticsActivity,
                        AdminDashboardActivity::class.java
                    )
                )
                finish()
            }
        }

        mainLayout.addView(
            backButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(mainLayout)

        loadStatistics(statisticsLayout)
    }

    private fun loadStatistics(statisticsLayout: LinearLayout) {
        lifecycleScope.launch {

            val statistics = withContext(Dispatchers.IO) {
                val userDao = database.userDao()
                val propertyDao = database.propertyDao()
                val editDao = database.propertyEditRequestDao()
                val viewingDao = database.viewingDao()

                listOf(
                    "Total Registered Tenants" to
                            userDao.countByRole("Tenant"),

                    "Total Registered Property Owners" to
                            userDao.countByRole("Property Owner"),

                    "Total Properties" to
                            propertyDao.countAll(),

                    "Properties Awaiting Approval" to
                            propertyDao.countByStatus("PENDING"),

                    "Property Edit Requests Awaiting Review" to
                            editDao.getByStatus("PENDING").size,

                    "Total Scheduled Viewing Bookings" to
                            viewingDao.countAll()
                )
            }

            statisticsLayout.removeAllViews()

            statistics.forEach { (label, value) ->
                addStatistic(statisticsLayout, label, value)
            }
        }
    }

    private fun addStatistic(
        parent: LinearLayout,
        label: String,
        value: Int
    ) {
        val statisticText = TextView(this).apply {
            text = "$label: $value"
            textSize = 18f
            setPadding(16, 20, 16, 20)
        }

        parent.addView(
            statisticText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        )
    }
}