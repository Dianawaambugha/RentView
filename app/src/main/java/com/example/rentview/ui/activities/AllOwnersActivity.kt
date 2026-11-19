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

class AllOwnersActivity : AppCompatActivity() {

    private lateinit var database: RentViewDatabase
    private lateinit var ownersContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = RentViewDatabase.getInstance(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "All Registered Property Owners"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 24)
        }

        ownersContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val scrollView = ScrollView(this).apply {
            addView(ownersContainer)
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

        displayOwners()
    }

    private fun displayOwners() {
        lifecycleScope.launch {
            val owners = withContext(Dispatchers.IO) {
                database.userDao().getAllOwners()
            }

            ownersContainer.removeAllViews()

            if (owners.isEmpty()) {
                val emptyMessage = TextView(this@AllOwnersActivity).apply {
                    text = "No registered property owners found."
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setPadding(16, 40, 16, 40)
                }

                ownersContainer.addView(emptyMessage)
                return@launch
            }

            owners.forEach { owner ->
                addOwnerCard(owner)
            }
        }
    }

    private fun addOwnerCard(owner: UserEntity) {
        val ownerCard = TextView(this).apply {
            text = """
                Owner ID: ${owner.id}
                Name: ${owner.fullName}
                Email: ${owner.email}
                Phone: ${owner.phone}
                Account Status: ${owner.approvalStatus}
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
        ownersContainer.addView(ownerCard, params)
    }
}