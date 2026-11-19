package com.example.rentview.ui.activities

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.data.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class AllTenantsActivity : AppCompatActivity() {

    private lateinit var tenantsContainer: LinearLayout

    private val supabase by lazy {
        SupabaseClient.client
    }

    private data class Tenant(
        val id: String,
        val fullName: String,
        val email: String,
        val phone: String,
        val approvalStatus: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            setOnClickListener {
                finish()
            }
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

        tenantsContainer.removeAllViews()

        lifecycleScope.launch {
            try {
                val profiles = supabase
                    .from("profiles")
                    .select {
                        filter {
                            eq("role", "TENANT")
                        }
                    }
                    .decodeList<JsonObject>()

                val tenants = profiles.map { profile ->

                    Tenant(
                        id = profile["id"]
                            ?.jsonPrimitive?.content ?: "Unknown",

                        fullName = profile["full_name"]
                            ?.jsonPrimitive?.content ?: "Unknown",

                        email = profile["email"]
                            ?.jsonPrimitive?.content ?: "Not provided",

                        phone = profile["phone"]
                            ?.jsonPrimitive?.content ?: "Not provided",

                        approvalStatus = profile["approval_status"]
                            ?.jsonPrimitive?.content ?: "UNKNOWN"
                    )
                }

                tenantsContainer.removeAllViews()

                if (tenants.isEmpty()) {
                    tenantsContainer.addView(
                        TextView(this@AllTenantsActivity).apply {
                            text = "No registered tenants found."
                            textSize = 16f
                            gravity = Gravity.CENTER
                            setPadding(16, 40, 16, 40)
                        }
                    )
                    return@launch
                }

                tenants.forEach { tenant ->
                    addTenantCard(tenant)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@AllTenantsActivity,
                    "Unable to load tenants: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addTenantCard(tenant: Tenant) {

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
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, 16)

        tenantsContainer.addView(tenantCard, params)
    }
}