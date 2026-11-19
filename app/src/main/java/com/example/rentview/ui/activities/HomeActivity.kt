package com.example.rentview.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.SupabaseAuthRepository
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val ROLE_TENANT = "Tenant"
        private const val ROLE_OWNER = "Property Owner"
    }

    private val authRepository by lazy {
        SupabaseAuthRepository()
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // The app continues working whether permission is granted or denied.
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

        when (role) {
            ROLE_OWNER -> {
                dashboardTitle.text = "Property Owner Dashboard"
                primaryButton.text = "Add a Property"
                secondaryButton.text = "My Properties"
                profile.text =
                    "Manage your properties and view requests from potential tenants."
            }

            ROLE_TENANT -> {
                dashboardTitle.text = "Tenant Dashboard"
                primaryButton.text = "Explore Properties"
                secondaryButton.text = "My Viewings"
                profile.text =
                    "Discover rental properties and manage your viewing appointments."

                requestNotificationPermission()
            }

            else -> {
                Toast.makeText(
                    this,
                    "Invalid account role.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
                return
            }
        }

        primaryButton.setOnClickListener {
            when (role) {
                ROLE_OWNER -> {
                    val intent = Intent(this, AddPropertyActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                }

                ROLE_TENANT -> {
                    val intent = Intent(this, PropertyListActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                }
            }
        }

        secondaryButton.setOnClickListener {
            when (role) {
                ROLE_OWNER -> {
                    val intent = Intent(this, MyPropertiesActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                }

                ROLE_TENANT -> {
                    val intent = Intent(this, MyViewingsActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                }
            }
        }

        logoutButton.setOnClickListener {
            logoutButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    authRepository.logout()

                    Toast.makeText(
                        this@HomeActivity,
                        "You have been logged out.",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(
                        this@HomeActivity,
                        MainActivity::class.java
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }

                    startActivity(intent)
                    finish()

                } catch (exception: Exception) {
                    Toast.makeText(
                        this@HomeActivity,
                        exception.message ?: "Logout failed. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()

                    logoutButton.isEnabled = true
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )

            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }
}