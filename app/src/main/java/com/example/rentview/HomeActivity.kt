package com.example.rentvew

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Get the views from the home screen
        val greeting = findViewById<TextView>(R.id.tvHomeGreeting)
        val userRole = findViewById<TextView>(R.id.tvHomeRole)
        val dashboardTitle = findViewById<TextView>(R.id.tvDashboardTitle)
        val primaryButton = findViewById<Button>(R.id.btnHomePrimary)
        val secondaryButton = findViewById<Button>(R.id.btnHomeSecondary)
        val profile = findViewById<TextView>(R.id.tvHomeProfile)
        val logoutButton = findViewById<Button>(R.id.btnLogout)

        // Read the user's details passed from the login screen
        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        val role = intent.getStringExtra("USER_ROLE") ?: "Tenant"

        // Display the user's name and role
        greeting.text = "Welcome, $userName!"
        userRole.text = "Account type: $role"

        // Display different dashboard options for each role
        if (role == "Property Owner") {
            dashboardTitle.text = "Property Owner Dashboard"
            primaryButton.text = "Add a Property"
            secondaryButton.text = "My Properties"
            profile.text = "Manage your properties and view requests from potential tenants."
        } else {
            dashboardTitle.text = "Tenant Dashboard"
            primaryButton.text = "Explore Properties"
            secondaryButton.text = "My Viewings"
            profile.text = "Discover rental properties and manage your viewing appointments."
        }
// Open the Add Property screen for property owners
        primaryButton.setOnClickListener {
            if (role == "Property Owner") {
                val addPropertyIntent = Intent(this, AddPropertyActivity::class.java)
                startActivity(addPropertyIntent)
            } else {
                Toast.makeText(
                    this,
                    "Property browsing will be added next.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        secondaryButton.setOnClickListener {
            Toast.makeText(
                this,
                "This feature will be added next.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Log out and return to the welcome screen
        logoutButton.setOnClickListener {
            finish()
        }
    }
}