package com.example.rentview.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.RentViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val ROLE_TENANT = "Tenant"
        private const val ROLE_OWNER = "Property Owner"

        private const val STATUS_PENDING = "PENDING"
        private const val STATUS_APPROVED = "APPROVED"
        private const val STATUS_REJECTED = "REJECTED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.etLoginEmail)
        val password = findViewById<EditText>(R.id.etLoginPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerLink = findViewById<TextView>(R.id.tvGoToRegister)

        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {

            val emailValue = email.text.toString().trim()
            val passwordValue = password.text.toString()

            when {

                emailValue.isEmpty() -> {
                    email.error = "Enter your email address"
                }

                !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches() -> {
                    email.error = "Enter a valid email address"
                }

                passwordValue.isEmpty() -> {
                    password.error = "Enter your password"
                }

                else -> {

                    loginButton.isEnabled = false

                    lifecycleScope.launch {

                        try {

                            val user = withContext(Dispatchers.IO) {

                                val database = RentViewDatabase.getInstance(
                                    applicationContext
                                )

                                database.userDao().getByEmail(emailValue.lowercase())
                            }

                            if (user == null ||
                                user.passwordHash != hashPassword(passwordValue)
                            ) {

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Incorrect email or password.",
                                    Toast.LENGTH_LONG
                                ).show()

                            } else {

                                when {

                                    user.role == ROLE_OWNER &&
                                            user.approvalStatus == STATUS_PENDING -> {

                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Your account is awaiting administrator approval.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    user.role == ROLE_OWNER &&
                                            user.approvalStatus == STATUS_REJECTED -> {

                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Your owner account was not approved. Please contact the administrator.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    user.role == ROLE_TENANT ||
                                            (user.role == ROLE_OWNER &&
                                                    user.approvalStatus == STATUS_APPROVED) -> {

                                        val homeIntent = Intent(
                                            this@LoginActivity,
                                            HomeActivity::class.java
                                        )

                                        homeIntent.putExtra("USER_ID", user.id)
                                        homeIntent.putExtra("USER_NAME", user.fullName)
                                        homeIntent.putExtra("USER_ROLE", user.role)
                                        homeIntent.putExtra(
                                            "APPROVAL_STATUS",
                                            user.approvalStatus
                                        )

                                        startActivity(homeIntent)
                                        finish()
                                    }

                                    else -> {

                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Your account status is invalid. Please contact the administrator.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }

                        } catch (e: Exception) {

                            Toast.makeText(
                                this@LoginActivity,
                                "Login failed: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()

                        } finally {

                            loginButton.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun hashPassword(password: String): String {

        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(password.toByteArray())

        return bytes.joinToString("") {
            "%02x".format(it)
        }
    }
}