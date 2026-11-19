package com.example.rentview.ui.activities

import android.os.Bundle
import android.util.Patterns
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.SupabaseAuthRepository
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private val authRepository by lazy {
        SupabaseAuthRepository()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val fullName = findViewById<EditText>(R.id.etFullName)
        val email = findViewById<EditText>(R.id.etEmail)
        val phone = findViewById<EditText>(R.id.etPhone)
        val password = findViewById<EditText>(R.id.etPassword)
        val role = findViewById<Spinner>(R.id.spinnerRole)

        role.setSelection(0)

        role.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedRole =
                        parent?.getItemAtPosition(position).toString()

                    android.util.Log.d(
                        "RentView",
                        "Selected role: $selectedRole"
                    )
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }

        val registerButton = findViewById<Button>(R.id.btnRegister)
        val loginLink = findViewById<TextView>(R.id.tvGoToLogin)

        loginLink.setOnClickListener {
            val intent = android.content.Intent(
                this,
                LoginActivity::class.java
            )
            startActivity(intent)
        }

        registerButton.setOnClickListener {

            val nameValue = fullName.text.toString().trim()
            val emailValue = email.text.toString().trim()
            val phoneValue = phone.text.toString().trim()
            val passwordValue = password.text.toString()
            val roleValue = role.selectedItem.toString()

            when {
                nameValue.isEmpty() -> {
                    fullName.error = "Enter your full name"
                }

                emailValue.isEmpty() -> {
                    email.error = "Enter your email"
                }

                !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches() -> {
                    email.error = "Enter a valid email address"
                }

                phoneValue.isEmpty() -> {
                    phone.error = "Enter your phone number"
                }

                passwordValue.length < 8 -> {
                    password.error =
                        "Password must have at least 8 characters"
                }

                roleValue == "Select your role" -> {
                    Toast.makeText(
                        this,
                        "Please select your role",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {

                    val cloudRole = when (roleValue) {
                        "Tenant" -> "TENANT"
                        "Property Owner" -> "PROPERTY_OWNER"
                        else -> null
                    }

                    if (cloudRole == null) {
                        Toast.makeText(
                            this,
                            "Please select a valid role",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    registerButton.isEnabled = false

                    lifecycleScope.launch {

                        try {
                            authRepository.register(
                                fullName = nameValue,
                                email = emailValue,
                                phone = phoneValue,
                                password = passwordValue,
                                role = cloudRole
                            )

                            if (cloudRole == "PROPERTY_OWNER") {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Registration successful! Your account is pending administrator approval.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Account created successfully! Check your email if verification is required, then log in.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            fullName.text.clear()
                            email.text.clear()
                            phone.text.clear()
                            password.text.clear()
                            role.setSelection(0)

                        } catch (exception: Exception) {

                            android.util.Log.e(
                                "RentView",
                                "Supabase registration failed",
                                exception
                            )

                            Toast.makeText(
                                this@RegisterActivity,
                                exception.message
                                    ?: "Registration failed. Please try again.",
                                Toast.LENGTH_LONG
                            ).show()

                        } finally {
                            registerButton.isEnabled = true
                        }
                    }
                }
            }
        }
    }
}