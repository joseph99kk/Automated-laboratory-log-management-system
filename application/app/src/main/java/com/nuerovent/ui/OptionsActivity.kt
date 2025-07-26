package com.nuerovent.ui

import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.nuerovent.R
import com.nuerovent.databinding.ActivityOptionsBinding
import com.nuerovent.ui.audit.AuditActivity
import okhttp3.*
import java.io.IOException

class Options : AppCompatActivity() {

    private lateinit var binding: ActivityOptionsBinding
    private val client = OkHttpClient()
    private val esp32BaseUrl = "http://192.168.4.1" //IP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupOptions()
    }

    private fun setupOptions() {
        val options = listOf(
            Pair("Audit checklist Form", R.drawable.file),
            Pair("Log Out", R.drawable.log_out),
            Pair("Add User", R.drawable.user),
            Pair("Reboot System", R.drawable.reboot)
        )

        for ((index, option) in options.withIndex()) {
            val (text, iconRes) = option
            val itemView = LayoutInflater.from(this).inflate(R.layout.option_item, binding.optionsContainer, false) as LinearLayout

            itemView.findViewById<ImageView>(R.id.optionIcon).setImageResource(iconRes)
            itemView.findViewById<TextView>(R.id.optionText).text = text
            binding.optionsContainer.addView(itemView)

            itemView.setOnClickListener {
                when (index) {
                    0 -> startActivity(Intent(this, AuditActivity::class.java))
                    1 -> {
                        val intent = Intent(this, com.nuerovent.ui.auth.MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    2 -> showAddUserDialog()
                    3 -> {
                        Toast.makeText(this, "Reboot command not implemented yet", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showAddUserDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null)
        val emailInput = dialogView.findViewById<EditText>(R.id.emailInput)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Add User")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()
                sendUserToESP(email, password)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendUserToESP(email: String, password: String) {
        val url = "$esp32BaseUrl/add_user?email=$email&password=$password"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@Options, "Failed to connect to ESP", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    val message = if (response.isSuccessful) "User sent to ESP!" else "ESP rejected user"
                    Toast.makeText(this@Options, message, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
