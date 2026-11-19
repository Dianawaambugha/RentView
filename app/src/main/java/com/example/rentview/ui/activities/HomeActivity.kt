package com.example.rentview.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.rentview.R

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val ROLE_TENANT = "Tenant"
        private const val ROLE_OWNER = "Property Owner"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val greeting = findViewById<TextView>(R.id.tvHomeGreeting)
        val userRole = findViewById<TextView>(R.id.tvHomeRole)
        val dashboardTitle = findViewById<TextView>(R.id.tvDashboardTitle)
        val primaryButton = findViewById<Button>(R.id.btnHomePrimary)
        val secondaryButton = findViewById<Button>(R.id.btnHomeSecondary)
        val profile = findViewById<TextView>(R.id.tvHomeProfile)
        val logoutButton = findViewById<Button>(R.id.btnLogout)

        val userName = intent.getStringExtra("USER_NAME") ?: "User"
        val role = intent.getStringExtra("USER_ROLE") ?: ROLE_TENANT
        val userId = intent.getLongExtra("USER_ID", -1L)

        greeting.text = "Welcome, $userName!"
        userRole.text = "Account type: $role"

        if (role == ROLE_OWNER) {

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

        primaryButton.setOnClickListener {

            if (role == ROLE_OWNER) {

                val addPropertyIntent = Intent(
                    this,
                    AddPropertyActivity::class.java
                )

                addPropertyIntent.putExtra("USER_ID", userId)
                startActivity(addPropertyIntent)

            } else if (role == ROLE_TENANT) {

                val propertyListIntent = Intent(
                    this,
                    PropertyListActivity::class.java
                )

                propertyListIntent.putExtra("USER_ID", userId)
                startActivity(propertyListIntent)
            }
        }

        secondaryButton.setOnClickListener {

            if (role == ROLE_OWNER) {

                val propertiesIntent = Intent(
                    this,
                    MyPropertiesActivity::class.java
                )

                propertiesIntent.putExtra("USER_ID", userId)
                startActivity(propertiesIntent)

            } else if (role == ROLE_TENANT) {

                val viewingsIntent = Intent(
                    this,
                    MyViewingsActivity::class.java
                )

                viewingsIntent.putExtra("USER_ID", userId)
                startActivity(viewingsIntent)
            }
        }

        logoutButton.setOnClickListener {
            finish()
        }
    }
}