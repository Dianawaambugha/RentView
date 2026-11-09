package com.example.rentview.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.rentview.R
import com.example.rentview.data.UserDatabaseHelper

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
        val role = intent.getStringExtra("USER_ROLE") ?: UserDatabaseHelper.ROLE_TENANT
        val userId = intent.getLongExtra("USER_ID", -1L)

        // Display the user's name and role
        greeting.text = "Welcome, $userName!"
        userRole.text = "Account type: $role"

        // Display different dashboard options for each role
        if (role == UserDatabaseHelper.ROLE_OWNER) {

            dashboardTitle.text = "Property Owner Dashboard"
            primaryButton.text = "Add a Property"
            secondaryButton.text = "My Properties"
            profile.text =
                "Manage your properties and view requests from potential tenants."

        } else {

            dashboardTitle.text = "Tenant Dashboard"
            primaryButton.text = "Explore Properties"
            secondaryButton.text = "My Viewings"
            profile.text =
                "Discover rental properties and manage your viewing appointments."
        }

        // Primary button: Add Property for owners, Explore Properties for tenants
        primaryButton.setOnClickListener {

            if (role == UserDatabaseHelper.ROLE_OWNER) {

                val addPropertyIntent = Intent(
                    this,
                    AddPropertyActivity::class.java
                )

                addPropertyIntent.putExtra("USER_ID", userId)
                startActivity(addPropertyIntent)

            } else if (role == UserDatabaseHelper.ROLE_TENANT) {

                val propertyListIntent = Intent(
                    this,
                    PropertyListActivity::class.java
                )

                propertyListIntent.putExtra("USER_ID", userId)
                startActivity(propertyListIntent)
            }
        }

        // Secondary button: My Properties for owners, My Viewings for tenants
        secondaryButton.setOnClickListener {

            if (role == UserDatabaseHelper.ROLE_OWNER) {

                val propertiesIntent = Intent(
                    this,
                    MyPropertiesActivity::class.java
                )

                propertiesIntent.putExtra("USER_ID", userId)
                startActivity(propertiesIntent)

            } else if (role == UserDatabaseHelper.ROLE_TENANT) {

                val viewingsIntent = Intent(
                    this,
                    MyViewingsActivity::class.java
                )

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