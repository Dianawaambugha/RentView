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
import com.example.rentview.data.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

class DashboardStatisticsActivity : AppCompatActivity() {

    private val supabase by lazy {
        SupabaseClient.client
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        statisticsLayout.removeAllViews()

        lifecycleScope.launch {
            try {

                // Count registered tenants.
                val tenants = supabase
                    .from("profiles")
                    .select {
                        filter {
                            eq("role", "TENANT")
                        }
                    }
                    .decodeList<JsonObject>()
                    .size

                // Count registered property owners.
                val owners = supabase
                    .from("profiles")
                    .select {
                        filter {
                            eq("role", "PROPERTY_OWNER")
                        }
                    }
                    .decodeList<JsonObject>()
                    .size

                // Count all properties.
                val properties = supabase
                    .from("properties")
                    .select()
                    .decodeList<JsonObject>()
                    .size

                // Count properties awaiting approval.
                val pendingProperties = supabase
                    .from("properties")
                    .select {
                        filter {
                            eq("property_status", "PENDING")
                        }
                    }
                    .decodeList<JsonObject>()
                    .size

                // Count pending property edit requests.
                val pendingEdits = supabase
                    .from("property_edit_requests")
                    .select {
                        filter {
                            eq("edit_status", "PENDING")
                        }
                    }
                    .decodeList<JsonObject>()
                    .size

                // Count all viewing bookings.
                val viewings = supabase
                    .from("viewings")
                    .select()
                    .decodeList<JsonObject>()
                    .size

                val statistics = listOf(
                    "Total Registered Tenants" to tenants,
                    "Total Registered Property Owners" to owners,
                    "Total Properties" to properties,
                    "Properties Awaiting Approval" to pendingProperties,
                    "Property Edit Requests Awaiting Review" to pendingEdits,
                    "Total Scheduled Viewing Bookings" to viewings
                )

                statisticsLayout.removeAllViews()

                statistics.forEach { (label, value) ->
                    addStatistic(statisticsLayout, label, value)
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@DashboardStatisticsActivity,
                    "Unable to load statistics: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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