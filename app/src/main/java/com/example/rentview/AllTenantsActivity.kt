package com.example.rentview

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AllTenantsActivity : AppCompatActivity() {
    private lateinit var databaseHelper: UserDatabaseHelper
    private lateinit var tenantsContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = UserDatabaseHelper.getInstance(this)

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
        tenantsContainer.removeAllViews()

        val cursor = databaseHelper.getAllTenants()

        cursor.use {
            if (!it.moveToFirst()) {
                val emptyMessage = TextView(this).apply {
                    text = "No registered tenants found."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(16, 40, 16, 40)
                }

                tenantsContainer.addView(emptyMessage)
                return
            }
            do {
                val tenantId = it.getLong(
                    it.getColumnIndexOrThrow(UserDatabaseHelper.COL_ID)
                )

                val name = it.getString(
                    it.getColumnIndexOrThrow(UserDatabaseHelper.COL_NAME)
                )

                val email = it.getString(
                    it.getColumnIndexOrThrow(UserDatabaseHelper.COL_EMAIL)
                )

                val phone = it.getString(
                    it.getColumnIndexOrThrow(UserDatabaseHelper.COL_PHONE)
                )

                val status = it.getString(
                    it.getColumnIndexOrThrow(
                        UserDatabaseHelper.COL_APPROVAL_STATUS
                    )
                )
                val tenantCard = TextView(this).apply {
                    text = """
                        Tenant ID: $tenantId
                        Name: $name
                        Email: $email
                        Phone: $phone
                        Account Status: $status
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

            } while (it.moveToNext())
        }
    }
            }