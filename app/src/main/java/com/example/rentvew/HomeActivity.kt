package com.example.rentvew

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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
        val userId = intent.getLongExtra("USER_ID", -1L)

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

        // Owners add a property; tenants browse the property list
        primaryButton.setOnClickListener {
            if (role == "Property Owner") {
                val addPropertyIntent = Intent(this, AddPropertyActivity::class.java)
                addPropertyIntent.putExtra("USER_ID", userId)
                startActivity(addPropertyIntent)
            } else {
                val listIntent = Intent(this, PropertyListActivity::class.java)
                listIntent.putExtra("USER_ID", userId)
                startActivity(listIntent)
            }
        }

        // Tenants see their viewings; owner screen comes later
        secondaryButton.setOnClickListener {
            if (role == "Property Owner") {
                android.widget.Toast.makeText(
                    this,
                    "This feature will be added next.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                val viewingsIntent = Intent(this, MyViewingsActivity::class.java)
                viewingsIntent.putExtra("USER_ID", userId)
                startActivity(viewingsIntent)
            }
        }

        // Log out and return to the welcome screen
        logoutButton.setOnClickListener {
            finish()
        }
    }
}