package com.example.rentvew

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

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

                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(emailValue).matches() -> {
                    email.error = "Enter a valid email address"
                }

                passwordValue.isEmpty() -> {
                    password.error = "Enter your password"
                }

                else -> {
                    val databaseHelper = UserDatabaseHelper(this)

                    val cursor = databaseHelper.getUserDetails(
                        emailValue,
                        passwordValue
                    )

                    if (cursor != null) {
                        cursor.use {

                            if (it.moveToFirst()) {

                                val userId = it.getLong(
                                    it.getColumnIndexOrThrow(
                                        UserDatabaseHelper.COL_ID
                                    )
                                )

                                val userName = it.getString(
                                    it.getColumnIndexOrThrow(
                                        UserDatabaseHelper.COL_NAME
                                    )
                                )

                                val userRole = it.getString(
                                    it.getColumnIndexOrThrow(
                                        UserDatabaseHelper.COL_ROLE
                                    )
                                )

                                val approvalStatus = it.getString(
                                    it.getColumnIndexOrThrow(
                                        UserDatabaseHelper.COL_APPROVAL_STATUS
                                    )
                                )

                                when {
                                    userRole == UserDatabaseHelper.ROLE_OWNER &&
                                            approvalStatus == UserDatabaseHelper.STATUS_PENDING -> {

                                        Toast.makeText(
                                            this,
                                            "Your account is awaiting administrator approval.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    userRole == UserDatabaseHelper.ROLE_OWNER &&
                                            approvalStatus == UserDatabaseHelper.STATUS_REJECTED -> {

                                        Toast.makeText(
                                            this,
                                            "Your owner account was not approved. Please contact the administrator.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    userRole == UserDatabaseHelper.ROLE_TENANT ||
                                            (userRole == UserDatabaseHelper.ROLE_OWNER &&
                                                    approvalStatus == UserDatabaseHelper.STATUS_APPROVED) -> {

                                        val homeIntent = Intent(
                                            this,
                                            HomeActivity::class.java
                                        )

                                        homeIntent.putExtra("USER_ID", userId)
                                        homeIntent.putExtra("USER_NAME", userName)
                                        homeIntent.putExtra("USER_ROLE", userRole)
                                        homeIntent.putExtra(
                                            "APPROVAL_STATUS",
                                            approvalStatus
                                        )

                                        startActivity(homeIntent)
                                        finish()
                                    }

                                    else -> {
                                        Toast.makeText(
                                            this,
                                            "Your account status is invalid. Please contact the administrator.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }

                            } else {
                                Toast.makeText(
                                    this,
                                    "Incorrect email or password.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    databaseHelper.close()
                }
            }
        }
    }
}
