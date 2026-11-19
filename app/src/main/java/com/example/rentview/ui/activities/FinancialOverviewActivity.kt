package com.example.rentview.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

class FinancialOverviewActivity : AppCompatActivity() {

    private lateinit var tvApprovedPropertyCount: TextView
    private lateinit var tvTotalAdvertisedRent: TextView
    private lateinit var tvAverageAdvertisedRent: TextView

    private val supabase by lazy {
        SupabaseClient.client
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_overview)

        tvApprovedPropertyCount =
            findViewById(R.id.tvApprovedPropertyCount)

        tvTotalAdvertisedRent =
            findViewById(R.id.tvTotalAdvertisedRent)

        tvAverageAdvertisedRent =
            findViewById(R.id.tvAverageAdvertisedRent)

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadFinancialOverview()
    }

    private fun loadFinancialOverview() {

        lifecycleScope.launch {
            try {
                val approvedProperties = supabase
                    .from("properties")
                    .select {
                        filter {
                            eq("property_status", "APPROVED")
                        }
                    }
                    .decodeList<JsonObject>()

                val propertyCount = approvedProperties.size

                val totalRent = approvedProperties.sumOf { property ->
                    property["rent"]
                        ?.jsonPrimitive?.doubleOrNull ?: 0.0
                }

                val averageRent = if (propertyCount > 0) {
                    totalRent / propertyCount
                } else {
                    0.0
                }

                tvApprovedPropertyCount.text =
                    propertyCount.toString()

                tvTotalAdvertisedRent.text =
                    "KSh ${
                        String.format(
                            Locale.getDefault(),
                            "%,.2f",
                            totalRent
                        )
                    }"

                tvAverageAdvertisedRent.text =
                    "KSh ${
                        String.format(
                            Locale.getDefault(),
                            "%,.2f",
                            averageRent
                        )
                    }"

            } catch (e: Exception) {
                Toast.makeText(
                    this@FinancialOverviewActivity,
                    "Unable to load financial overview: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}