package com.example.rentview

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class FinancialOverviewActivity : AppCompatActivity() {

    private lateinit var tvApprovedPropertyCount: TextView
    private lateinit var tvTotalAdvertisedRent: TextView
    private lateinit var tvAverageAdvertisedRent: TextView

    private lateinit var dbHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financial_overview)

        dbHelper = UserDatabaseHelper(this)

        tvApprovedPropertyCount = findViewById(R.id.tvApprovedPropertyCount)
        tvTotalAdvertisedRent = findViewById(R.id.tvTotalAdvertisedRent)
        tvAverageAdvertisedRent = findViewById(R.id.tvAverageAdvertisedRent)

        // Back button: close this screen and return to the admin dashboard
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }

        loadFinancialOverview()
    }

    private fun loadFinancialOverview() {

        var propertyCount = 0
        var totalRent = 0.0

        dbHelper.getApprovedProperties().use {

            val rentColumn = it.getColumnIndexOrThrow(UserDatabaseHelper.COL_PROPERTY_RENT)

            while (it.moveToNext()) {
                propertyCount++
                totalRent += it.getDouble(rentColumn)
            }
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

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }
}