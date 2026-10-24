package com.example.rentview

import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class OwnerApprovalsActivity : AppCompatActivity() {
    private lateinit var ownerList: LinearLayout
    private lateinit var databaseHelper: UserDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databaseHelper = UserDatabaseHelper(this)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(24, 24, 24, 24)
        root.setBackgroundColor(0xFFF5F7FA.toInt())

        val title = TextView(this)
        title.text = "Property Owner Approvals"
        title.textSize = 25f
        title.setTextColor(0xFF17365D.toInt())
        title.setPadding(0, 0, 0, 20)
        root.addView(title)

        val scrollView = ScrollView(this)

        ownerList = LinearLayout(this)
        ownerList.orientation = LinearLayout.VERTICAL

        scrollView.addView(ownerList)
        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val backButton = Button(this)
        backButton.text = "Back to Dashboard"
        backButton.setOnClickListener {
            finish()
        }

        root.addView(backButton)

        setContentView(root)

        loadPendingOwners()
    }

    private fun loadPendingOwners() {

        ownerList.removeAllViews()

        val cursor = databaseHelper.getPendingOwners()

        cursor.use {

            if (it.count == 0) {
                val emptyMessage = TextView(this)
                emptyMessage.text = "There are no owners awaiting approval."
                emptyMessage.textSize = 17f
                emptyMessage.setPadding(0, 20, 0, 20)
                ownerList.addView(emptyMessage)
                return
            }

            while (it.moveToNext()) {

                val ownerId = it.getLong(
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

                addOwnerCard(ownerId, name, email, phone)
            }
        }
    }

    private fun addOwnerCard(
        ownerId: Long,
        name: String,
        email: String,
        phone: String
    ) {

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(18, 18, 18, 18)
        card.setBackgroundColor(0xFFFFFFFF.toInt())

        val details = TextView(this)
        details.text = "Name: $name\nEmail: $email\nPhone: $phone\nStatus: PENDING"
        details.textSize = 16f
        details.setTextColor(0xFF222222.toInt())
        details.setPadding(0, 0, 0, 12)

        card.addView(details)

        val buttons = LinearLayout(this)
        buttons.orientation = LinearLayout.HORIZONTAL
        buttons.gravity = Gravity.CENTER

        val approveButton = Button(this)
        approveButton.text = "Approve"

        approveButton.setOnClickListener {
            confirmDecision(ownerId, name, UserDatabaseHelper.STATUS_APPROVED)
        }

        val rejectButton = Button(this)
        rejectButton.text = "Reject"

        rejectButton.setOnClickListener {
            confirmDecision(ownerId, name, UserDatabaseHelper.STATUS_REJECTED)
        }

        buttons.addView(
            approveButton,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        buttons.addView(
            rejectButton,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        )

        card.addView(buttons)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        cardParams.setMargins(0, 0, 0, 20)

        ownerList.addView(card, cardParams)
    }

    private fun confirmDecision(
        ownerId: Long,
        ownerName: String,
        status: String
    ) {

        val action = if (status == UserDatabaseHelper.STATUS_APPROVED) {
            "approve"
        } else {
            "reject"
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Decision")
            .setMessage("Are you sure you want to $action $ownerName?")
            .setPositiveButton("Confirm") { _, _ ->

                val updated = databaseHelper.updateOwnerApproval(
                    ownerId,
                    status
                )

                if (updated) {

                    val message = if (
                        status == UserDatabaseHelper.STATUS_APPROVED
                    ) {
                        "$ownerName has been approved."
                    } else {
                        "$ownerName has been rejected."
                    }

                    Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                    ).show()

                    loadPendingOwners()

                } else {

                    Toast.makeText(
                        this,
                        "Unable to update the owner's status.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        databaseHelper.close()
        super.onDestroy()
    }

}
