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
import com.example.rentview.data.RentViewDatabase
import com.example.rentview.data.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {

    private val database by lazy {
        RentViewDatabase.getInstance(applicationContext)
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
                    password.error = "Password must have at least 8 characters"
                }

                roleValue == "Select your role" -> {
                    Toast.makeText(
                        this,
                        "Please select your role",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    registerButton.isEnabled = false

                    lifecycleScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            try {
                                val existingUser =
                                    database.userDao().getByEmail(emailValue)

                                if (existingUser != null) {
                                    false
                                } else {
                                    val passwordHash = hashPassword(passwordValue)

                                    val approvalStatus =
                                        if (roleValue == "Property Owner") {
                                            "PENDING"
                                        } else {
                                            "APPROVED"
                                        }

                                    val newUser = UserEntity(
                                        fullName = nameValue,
                                        email = emailValue,
                                        phone = phoneValue,
                                        passwordHash = passwordHash,
                                        role = roleValue,
                                        approvalStatus = approvalStatus
                                    )

                                    database.userDao().insert(newUser)
                                    true
                                }
                            } catch (exception: Exception) {
                                android.util.Log.e(
                                    "RentView",
                                    "Registration failed",
                                    exception
                                )
                                false
                            }
                        }

                        registerButton.isEnabled = true

                        if (result) {

                             if (roleValue == "Property Owner") {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Registration successful! Your account is pending administrator approval. You cannot list properties until approved.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    "Account created successfully! You can now log in.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            fullName.text.clear()
                            email.text.clear()
                            phone.text.clear()
                            password.text.clear()
                            role.setSelection(0)

                        } else {
                            email.error =
                                "This email may already be registered"

                            Toast.makeText(
                                this@RegisterActivity,
                                "Registration failed. This email may already exist.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}