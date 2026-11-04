package com.example.rentview

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

        // Read the user's details passed from LoginActivity
        val userId = intent.getLongExtra("USER_ID", -1L)
        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        val role = intent.getStringExtra("USER_ROLE") ?: "Tenant"

        // Display the user's name and role
        greeting.text = "Welcome, $userName!"
        userRole.text = "Account type: $role"

        // Display dashboard options according to the user's role
        if (role == "Property Owner") {
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

        // Primary button: Add a Property or Explore Properties
        primaryButton.setOnClickListener {
            if (role == "Property Owner") {
                val addPropertyIntent =
                    Intent(this, AddPropertyActivity::class.java)

                addPropertyIntent.putExtra("USER_ID", userId)
                addPropertyIntent.putExtra("USER_NAME", userName)
                addPropertyIntent.putExtra("USER_ROLE", role)

                startActivity(addPropertyIntent)
            } else {
                val propertyListIntent =
                    Intent(this, PropertyListActivity::class.java)

                propertyListIntent.putExtra("USER_ID", userId)
                propertyListIntent.putExtra("USER_NAME", userName)
                propertyListIntent.putExtra("USER_ROLE", role)

                startActivity(propertyListIntent)
            }
        }

        // Secondary button: My Properties or My Viewings
        secondaryButton.setOnClickListener {
            if (role == "Property Owner") {
                val myPropertiesIntent =
                    Intent(this, MyPropertiesActivity::class.java)

                myPropertiesIntent.putExtra("USER_ID", userId)
                myPropertiesIntent.putExtra("USER_NAME", userName)
                myPropertiesIntent.putExtra("USER_ROLE", role)

                startActivity(myPropertiesIntent)
            } else {
                val myViewingsIntent =
                    Intent(this, MyViewingsActivity::class.java)

                myViewingsIntent.putExtra("USER_ID", userId)
                myViewingsIntent.putExtra("USER_NAME", userName)
                myViewingsIntent.putExtra("USER_ROLE", role)

                startActivity(myViewingsIntent)
            }
        }

        // Log out and return to the previous screen
        logoutButton.setOnClickListener {
            finish()
        }
    }

}
