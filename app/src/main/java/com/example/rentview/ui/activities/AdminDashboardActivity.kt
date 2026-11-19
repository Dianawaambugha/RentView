package com.example.rentview.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.SupabaseAuthRepository
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private val authRepository by lazy {
        SupabaseAuthRepository()
    }

    private lateinit var dashboardButtons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        dashboardButtons = listOf(
            findViewById(R.id.btnOwnerApprovals),
            findViewById(R.id.btnPropertyApprovals),
            findViewById(R.id.btnDashboardStatistics),
            findViewById(R.id.btnAllProperties),
            findViewById(R.id.btnPropertyEditRequests),
            findViewById(R.id.btnAllViewings),
            findViewById(R.id.btnAllTenants),
            findViewById(R.id.btnAllOwners),
            findViewById(R.id.btnFinancialOverview)
        )

        dashboardButtons.forEach {
            it.isEnabled = false
        }

        verifyAdministratorAccess()
    }

    private fun verifyAdministratorAccess() {
        lifecycleScope.launch {
            try {
                val isAdmin = authRepository.isCurrentUserAdmin()

                if (!isAdmin) {
                    authRepository.logout()

                    Toast.makeText(
                        this@AdminDashboardActivity,
                        "Administrator access denied. Please log in again.",
                        Toast.LENGTH_LONG
                    ).show()

                    returnToMain()
                    return@launch
                }

                enableDashboardNavigation()

            } catch (exception: Exception) {
                Toast.makeText(
                    this@AdminDashboardActivity,
                    exception.message ?: "Unable to verify administrator access.",
                    Toast.LENGTH_LONG
                ).show()

                try {
                    authRepository.logout()
                } catch (_: Exception) {
                    // Continue to the main screen.
                }

                returnToMain()
            }
        }
    }

    private fun enableDashboardNavigation() {

        findViewById<Button>(R.id.btnOwnerApprovals).setOnClickListener {
            openScreen(OwnerApprovalsActivity::class.java)
        }

        findViewById<Button>(R.id.btnPropertyApprovals).setOnClickListener {
            openScreen(PropertyApprovalsActivity::class.java)
        }

        findViewById<Button>(R.id.btnDashboardStatistics).setOnClickListener {
            openScreen(DashboardStatisticsActivity::class.java)
        }

        findViewById<Button>(R.id.btnAllProperties).setOnClickListener {
            openScreen(AllPropertiesActivity::class.java)
        }

        findViewById<Button>(R.id.btnPropertyEditRequests).setOnClickListener {
            openScreen(PropertyEditRequestsActivity::class.java)
        }

        findViewById<Button>(R.id.btnAllViewings).setOnClickListener {
            openScreen(ViewingRequestsActivity::class.java)
        }

        findViewById<Button>(R.id.btnAllTenants).setOnClickListener {
            openScreen(AllTenantsActivity::class.java)
        }

        findViewById<Button>(R.id.btnAllOwners).setOnClickListener {
            openScreen(AllOwnersActivity::class.java)
        }

        findViewById<Button>(R.id.btnFinancialOverview).setOnClickListener {
            openScreen(FinancialOverviewActivity::class.java)
        }

        dashboardButtons.forEach {
            it.isEnabled = true
        }
    }

    private fun openScreen(activity: Class<*>) {
        startActivity(Intent(this, activity))
    }

    private fun returnToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}