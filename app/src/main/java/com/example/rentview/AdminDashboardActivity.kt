package com.example.rentview

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Approval buttons
        val ownerButton = findViewById<Button>(R.id.btnOwnerApprovals)
        val propertyButton = findViewById<Button>(R.id.btnPropertyApprovals)

        // Dashboard buttons
        val statisticsButton = findViewById<Button>(R.id.btnDashboardStatistics)
        val allPropertiesButton = findViewById<Button>(R.id.btnAllProperties)
        val editRequestsButton = findViewById<Button>(R.id.btnPropertyEditRequests)
        val allViewingsButton = findViewById<Button>(R.id.btnAllViewings)
        val allTenantsButton = findViewById<Button>(R.id.btnAllTenants)
        val allOwnersButton = findViewById<Button>(R.id.btnAllOwners)
        val financialButton = findViewById<Button>(R.id.btnFinancialOverview)

        // Review Property Owners
        ownerButton.setOnClickListener {
            startActivity(Intent(this, OwnerApprovalsActivity::class.java))
        }

        // Review Property Listings
        propertyButton.setOnClickListener {
            startActivity(Intent(this, PropertyApprovalsActivity::class.java))
        }

        // Dashboard Statistics
        statisticsButton.setOnClickListener {
            startActivity(Intent(this, DashboardStatisticsActivity::class.java))
        }

        // All Properties
        allPropertiesButton.setOnClickListener {
            startActivity(Intent(this, AllPropertiesActivity::class.java))
        }

        // Property Edit Requests
        editRequestsButton.setOnClickListener {
            startActivity(Intent(this, PropertyEditRequestsActivity::class.java))
        }

        // Scheduled Viewings
        allViewingsButton.setOnClickListener {
            startActivity(Intent(this, ViewingRequestsActivity::class.java))
        }

        // All Tenants
        allTenantsButton.setOnClickListener {
            startActivity(Intent(this, AllTenantsActivity::class.java))
        }

        // All Property Owners
        allOwnersButton.setOnClickListener {
            startActivity(Intent(this, AllOwnersActivity::class.java))
        }

        // Financial Overview
        financialButton.setOnClickListener {
            startActivity(Intent(this, FinancialOverviewActivity::class.java))
        }
    }
}
