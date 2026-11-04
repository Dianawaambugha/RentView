package com.example.rentview

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DashboardStatisticsActivity : AppCompatActivity() {

    private lateinit var databaseHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = UserDatabaseHelper.getInstance(this)

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

        addStatistic(
            statisticsLayout,
            "Total Registered Tenants",
            databaseHelper.getTotalTenants()
        )

        addStatistic(
            statisticsLayout,
            "Total Registered Property Owners",
            databaseHelper.getTotalOwners()
        )

        addStatistic(
            statisticsLayout,
            "Total Properties",
            databaseHelper.getTotalProperties()
        )

        addStatistic(
            statisticsLayout,
            "Properties Awaiting Approval",
            databaseHelper.getTotalPendingProperties()
        )

        addStatistic(
            statisticsLayout,
            "Property Edit Requests Awaiting Review",
            databaseHelper.getTotalPendingPropertyEdits()
        )

        addStatistic(
            statisticsLayout,
            "Total Scheduled Viewing Bookings",
            databaseHelper.getTotalViewings()
        )

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
