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

    data class Admin(val email: String, val phone: String)

    // ✅ Five hardcoded admin credentials (email + phone)
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

        val emailInput = findViewById<EditText>(R.id.emailInput)
        val phoneInput = findViewById<EditText>(R.id.securityCodeInput) // your layout uses this id
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val enteredEmail = emailInput.text.toString().trim()
            val enteredPhone = phoneInput.text.toString().trim()

            val matchedAdmin = adminCredentials.find {
                it.email.equals(enteredEmail, ignoreCase = true) && it.phone == enteredPhone
            }

            if (matchedAdmin != null) {
                Toast.makeText(this, "Welcome ${matchedAdmin.email}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
            } else {
                Toast.makeText(this, "Invalid email or code", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
