package com.example.rentvew

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.content.Intent

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val fullName = findViewById<EditText>(R.id.etFullName)
        val email = findViewById<EditText>(R.id.etEmail)
        val phone = findViewById<EditText>(R.id.etPhone)
        val password = findViewById<EditText>(R.id.etPassword)
        val role = findViewById<Spinner>(R.id.spinnerRole)
        role.setSelection(0)
        role.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val selectedRole = parent?.getItemAtPosition(position).toString()
                android.util.Log.d("RentView", "Selected role: $selectedRole")
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            }
        })
        val registerButton = findViewById<Button>(R.id.btnRegister)
        val loginLink = findViewById<TextView>(R.id.tvGoToLogin)

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        registerButton.setOnClickListener {

            val nameValue = fullName.text.toString().trim()
            val emailValue = email.text.toString().trim()
            val phoneValue = phone.text.toString().trim()
            val passwordValue = password.text.toString()
            val roleValue = role.selectedItem.toString()

            when {
                nameValue.isEmpty() ->
                    fullName.error = "Enter your full name"

                emailValue.isEmpty() ->
                    email.error = "Enter your email"

                !android.util.Patterns.EMAIL_ADDRESS
                    .matcher(emailValue).matches() ->
                    email.error = "Enter a valid email address"

                phoneValue.isEmpty() ->
                    phone.error = "Enter your phone number"

                passwordValue.length < 8 ->
                    password.error = "Password must have at least 8 characters"

                roleValue == "Select your role" ->
                    Toast.makeText(
                        this,
                        "Please select your role",
                        Toast.LENGTH_SHORT
                    ).show()

                else -> {
                    val databaseHelper = UserDatabaseHelper(this)

                    val saved = databaseHelper.registerUser(
                        name = nameValue,
                        email = emailValue,
                        phone = phoneValue,
                        password = passwordValue,
                        role = roleValue
                    )

                    if (saved) {

                        // Show a different message depending on the selected role
                        if (roleValue == UserDatabaseHelper.ROLE_OWNER) {
                            Toast.makeText(
                                this,
                                "Registration successful! Your account is pending administrator approval. You cannot list properties until approved.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Account created successfully! You can now log in.",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        // Clear the registration fields
                        fullName.text.clear()
                        email.text.clear()
                        phone.text.clear()
                        password.text.clear()
                        role.setSelection(0)

                    } else {

                        email.error = "This email may already be registered"

                        Toast.makeText(
                            this,
                            "Registration failed. This email may already exist.",
                            Toast.LENGTH_LONG
                        ).show()
                    }


                    databaseHelper.close()
                }
            }
        }
    }
}