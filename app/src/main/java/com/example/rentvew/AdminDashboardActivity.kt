package com.example.rentvew

import android.os.Bundle
import android.content.Intent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        val ownerButton = findViewById<Button>(R.id.btnOwnerApprovals)
        val propertyButton = findViewById<Button>(R.id.btnPropertyApprovals)

        ownerButton.setOnClickListener {
            val intent = Intent(this, OwnerApprovalsActivity::class.java)
            startActivity(intent)
        }

        propertyButton.setOnClickListener {
            // Property approval screen will be connected next.
        }
    }
}
