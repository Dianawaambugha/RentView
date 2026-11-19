package com.example.rentview.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class FinancialOverviewActivity : AppCompatActivity() {

    private lateinit var tvApprovedPropertyCount: TextView
    private lateinit var tvTotalAdvertisedRent: TextView
    private lateinit var tvAverageAdvertisedRent: TextView

    private lateinit var database: RentViewDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_overview)

        database = RentViewDatabase.getInstance(this)

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

            val (propertyCount, totalRent) = withContext(Dispatchers.IO) {

                val approvedProperties =
                    database.propertyDao().getApprovedProperties()

                val count = approvedProperties.size

                val total = approvedProperties.sumOf { it.rent }

                Pair(count, total)
            }

            val averageRent = if (propertyCount > 0) {
                totalRent / propertyCount
            } else {
                0.0
            }

            tvApprovedPropertyCount.text = propertyCount.toString()

            tvTotalAdvertisedRent.text =
                "KSh ${String.format(Locale.getDefault(), "%,.2f", totalRent)}"

            tvAverageAdvertisedRent.text =
                "KSh ${String.format(Locale.getDefault(), "%,.2f", averageRent)}"
        }
    }
}