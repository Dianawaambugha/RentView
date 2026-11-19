package com.example.rentview.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.rentview.R
import com.example.rentview.data.SupabaseAuthRepository
import kotlinx.coroutines.launch

class AdminLoginActivity : AppCompatActivity() {

    private val authRepository = SupabaseAuthRepository()

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

            loginButton.isEnabled = false

            lifecycleScope.launch {

                try {

                    val user = authRepository.login(
                        emailValue,
                        passwordValue
                    )

                    if (!user.role.equals("ADMIN", ignoreCase = true)) {

                        authRepository.logout()

                        Toast.makeText(
                            this@AdminLoginActivity,
                            "Invalid administrator email or password",
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }

                    if (!user.approvalStatus.equals(
                            "APPROVED",
                            ignoreCase = true
                        )
                    ) {

                        authRepository.logout()

                        Toast.makeText(
                            this@AdminLoginActivity,
                            "Administrator account is not approved.",
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }

                    Toast.makeText(
                        this@AdminLoginActivity,
                        "Administrator login successful",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(
                        this@AdminLoginActivity,
                        AdminDashboardActivity::class.java
                    )

                    intent.putExtra("USER_ID", user.id)
                    intent.putExtra("USER_NAME", user.fullName)
                    intent.putExtra("USER_ROLE", user.role)
                    intent.putExtra(
                        "APPROVAL_STATUS",
                        user.approvalStatus
                    )
                    intent.putExtra(
                        "SUPABASE_USER_ID",
                        user.supabaseUserId
                    )

                    startActivity(intent)
                    finish()

                } catch (e: Exception) {

                    Toast.makeText(
                        this@AdminLoginActivity,
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