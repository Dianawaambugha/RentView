package com.example.rentview

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminLoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        val email = findViewById<EditText>(R.id.etAdminEmail)
        val password = findViewById<EditText>(R.id.etAdminPassword)
        val loginButton = findViewById<Button>(R.id.btnAdminLogin)

        loginButton.setOnClickListener {

            val emailValue = email.text.toString().trim()
            val passwordValue = password.text.toString()

            if (emailValue.isEmpty()) {
                email.error = "Enter administrator email"
                return@setOnClickListener
            }

            if (passwordValue.isEmpty()) {
                password.error = "Enter administrator password"
                return@setOnClickListener
            }

            val adminEmail = "admin@rentview.com"
            val adminPassword = "Admin@123"

            if (
                emailValue.equals(adminEmail, ignoreCase = true) &&
                passwordValue == adminPassword
            ) {
                Toast.makeText(
                    this,
                    "Administrator login successful",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(
                    this,
                    AdminDashboardActivity::class.java
                )

                startActivity(intent)

            } else {
                Toast.makeText(
                    this,
                    "Invalid administrator email or password",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}
