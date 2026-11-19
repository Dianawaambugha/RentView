package com.example.rentview.ui.activities

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.RentViewDatabase
import com.example.rentview.data.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllTenantsActivity : AppCompatActivity() {

    private lateinit var database: RentViewDatabase
    private lateinit var tenantsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = RentViewDatabase.getInstance(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "All Registered Tenants"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }

        tenantsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            addView(tenantsContainer)
        }

        val backButton = Button(this).apply {
            text = "Back to Dashboard"
            setOnClickListener { finish() }
        }

        mainLayout.addView(title)

        mainLayout.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        mainLayout.addView(backButton)

        setContentView(mainLayout)

        displayTenants()
    }

    private fun displayTenants() {

        lifecycleScope.launch {

            val tenants = withContext(Dispatchers.IO) {
                database.userDao().getAllTenants()
            }

            tenantsContainer.removeAllViews()

            if (tenants.isEmpty()) {

                val emptyMessage = TextView(this@AllTenantsActivity).apply {
                    text = "No registered tenants found."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(16, 40, 16, 40)
                }

                tenantsContainer.addView(emptyMessage)
                return@launch
            }

            tenants.forEach { tenant ->
                addTenantCard(tenant)
            }
        }
    }

    private fun addTenantCard(tenant: UserEntity) {

        val tenantCard = TextView(this).apply {

            text = """
                Tenant ID: ${tenant.id}
                Name: ${tenant.fullName}
                Email: ${tenant.email}
                Phone: ${tenant.phone}
                Account Status: ${tenant.approvalStatus}
            """.trimIndent()

            textSize = 16f
            setPadding(20, 20, 20, 20)

            setBackgroundResource(
                android.R.drawable.dialog_holo_light_frame
            )
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, 16)

        tenantsContainer.addView(tenantCard, params)
    }
}