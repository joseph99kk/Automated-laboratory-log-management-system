package com.nuerovent.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nuerovent.R
import com.nuerovent.ui.DashboardActivity

class MainActivity : AppCompatActivity() {

    // ✅ Data class to hold admin credentials
    data class Admin(val email: String, val phone: String)

    // ✅ Predefined list of admin credentials
    private val adminCredentials = listOf(
        Admin("admin1@nuerovent.com", "1111"),
        Admin("admin2@nuerovent.com", "2222"),
        Admin("admin3@nuerovent.com", "3333"),
        Admin("admin4@nuerovent.com", "4444"),
        Admin("admin5@nuerovent.com", "5555")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ Reference UI elements
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val phoneInput = findViewById<EditText>(R.id.securityCodeInput)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val enteredEmail = emailInput.text.toString().trim()
            val enteredPhone = phoneInput.text.toString().trim()

            if (enteredEmail.isEmpty() || enteredPhone.isEmpty()) {
                Toast.makeText(this, "Please enter both email and code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isAdmin = adminCredentials.any {
                it.email.equals(enteredEmail, ignoreCase = true) && it.phone == enteredPhone
            }

            if (isAdmin) {
                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish() // Optional: prevent back-navigation to login screen
            } else {
                Toast.makeText(this, "Invalid email or code", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
