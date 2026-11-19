package com.example.rentview.ui.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
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

class OwnerApprovalsActivity : AppCompatActivity() {

    companion object {
        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_APPROVED = "APPROVED"
        private const val STATUS_REJECTED = "REJECTED"
        private const val ROLE_OWNER = "PROPERTY_OWNER"
    }

    private val supabase by lazy {
        SupabaseClient.client
    }

    private lateinit var ownerList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(0xFFF5F7FA.toInt())
        }

        val title = TextView(this).apply {
            text = "Property Owner Approvals"
            textSize = 25f
            setTextColor(0xFF17365D.toInt())
            setPadding(0, 0, 0, 20)
        }

        root.addView(title)

        val scrollView = ScrollView(this)

        ownerList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(ownerList)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val backButton = Button(this).apply {
            text = "Back to Dashboard"
            setOnClickListener {
                startActivity(
                    Intent(
                        this@OwnerApprovalsActivity,
                        AdminDashboardActivity::class.java
                    )
                )
                finish()
            }
        }

        root.addView(backButton)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()

        if (::ownerList.isInitialized) {
            loadPendingOwners()
        }
    }

    private fun loadPendingOwners() {
        ownerList.removeAllViews()

        lifecycleScope.launch {
            try {
                val pendingOwners = supabase
                    .from("profiles")
                    .select {
                        filter {
                            eq("role", ROLE_OWNER)
                            eq("approval_status", STATUS_PENDING)
                        }
                    }
                    .decodeList<JsonObject>()

                if (pendingOwners.isEmpty()) {
                    ownerList.addView(
                        TextView(this@OwnerApprovalsActivity).apply {
                            text = "There are no owners awaiting approval."
                            textSize = 17f
                            setPadding(0, 20, 0, 20)
                        }
                    )
                    return@launch
                }

                pendingOwners.forEach { owner ->
                    val id = owner["id"]?.jsonPrimitive?.content
                        ?: return@forEach

                    val name = owner["full_name"]?.jsonPrimitive?.content
                        ?: "Unknown name"

                    val email = owner["email"]?.jsonPrimitive?.content
                        ?: "Unavailable"

                    val phone = owner["phone"]?.jsonPrimitive?.content
                        ?: "Unavailable"

                    addOwnerCard(id, name, email, phone)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@OwnerApprovalsActivity,
                    "Unable to load pending owners: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addOwnerCard(
        ownerId: String,
        name: String,
        email: String,
        phone: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }

        val details = TextView(this).apply {
            text = "Name: $name\nEmail: $email\nPhone: $phone\nStatus: PENDING"
            textSize = 16f
            setTextColor(0xFF222222.toInt())
            setPadding(0, 0, 0, 12)
        }

        card.addView(details)

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val approveButton = Button(this).apply {
            text = "Approve"
        }

        val rejectButton = Button(this).apply {
            text = "Reject"
        }

        approveButton.setOnClickListener {
            confirmDecision(ownerId, name, STATUS_APPROVED)
        }

        rejectButton.setOnClickListener {
            confirmDecision(ownerId, name, STATUS_REJECTED)
        }

        buttons.addView(
            approveButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        buttons.addView(
            rejectButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        card.addView(buttons)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 20
        }

        ownerList.addView(card, cardParams)
    }

    private fun confirmDecision(
        ownerId: String,
        ownerName: String,
        status: String
    ) {
        val action = if (status == STATUS_APPROVED) {
            "approve"
        } else {
            "reject"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Decision")
            .setMessage("Are you sure you want to $action $ownerName?")
            .setPositiveButton("Confirm") { _, _ ->

                lifecycleScope.launch {
                    try {
                        supabase.from("profiles").update(
                            {
                                set("approval_status", status)
                            }
                        ) {
                            filter {
                                eq("id", ownerId)
                                eq("role", ROLE_OWNER)
                                eq("approval_status", STATUS_PENDING)
                            }
                        }

                        Toast.makeText(
                            this@OwnerApprovalsActivity,
                            "$ownerName has been ${if (status == STATUS_APPROVED) "approved" else "rejected"}.",
                            Toast.LENGTH_LONG
                        ).show()

                        loadPendingOwners()

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@OwnerApprovalsActivity,
                            "Approval update failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}