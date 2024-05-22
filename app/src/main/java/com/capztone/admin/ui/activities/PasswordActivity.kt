package com.capztone.admin.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.MainActivity
import com.capztone.admin.databinding.ActivityPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException
import kotlin.random.Random

class PasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    @SuppressLint("SuspiciousIndentation")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference
        val username = intent.getStringExtra("USERNAME")
        val email = intent.getStringExtra("EMAIL")

        // Set retrieved data to TextViews
        binding.textViewUsername.text = "Username: $username"
        binding.textViewEmail.text = "Email: $email"

        // Disable the button initially
        binding.btnSubmit.isEnabled = false

        // Add text change listeners to both password and address fields
        binding.editTextPassword.addTextChangedListener(passwordTextWatcher)
        binding.address.addTextChangedListener(addressTextWatcher)

        binding.btnSubmit.setOnClickListener {
            val password = binding.editTextPassword.text.toString()
            val address = binding.address.text.toString()

            // Check if the password is valid
            if (password.isNotBlank() && address.isNotBlank()) {
                saveUserInfo(password, address)

            } else {
                Toast.makeText(this, "Please enter both password and address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // TextWatcher for password field
    private val passwordTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Enable the button only if both password and address fields are filled
            binding.btnSubmit.isEnabled = s.toString().isNotEmpty() && binding.address.text.toString().isNotBlank()
        }
    }

    // TextWatcher for address field
    private val addressTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Enable the button only if both password and address fields are filled
            binding.btnSubmit.isEnabled = s.toString().isNotBlank() && binding.editTextPassword.text.toString().isNotEmpty()
        }
    }

    private fun saveUserInfo(password: String, address: String) {
        val user = auth.currentUser
        val username = intent.getStringExtra("USERNAME") ?: ""
        val email = intent.getStringExtra("EMAIL") ?: ""

        // List of allowed passwords
        val allowedPasswords = listOf("Admin1", "Admin2", "Admin3", "Admin4", "Admin5", "Admin6")

        if (password in allowedPasswords) {
            val passwordEntered = password

            // Map of passwords to shop names
            val passwordToShopName = mapOf(
                "Admin1" to "Shop 1",
                "Admin2" to "Shop 2",
                "Admin3" to "Shop 3",
                "Admin4" to "Shop 4",
                "Admin5" to "Shop 5",
                "Admin6" to "Shop 6"
            )

            val shopName = passwordToShopName[passwordEntered] ?: ""

            val userId = user?.uid ?: ""
            val userInfo = hashMapOf(
                "username" to username,
                "email" to email,
                "password" to passwordEntered,
                "shopName" to shopName
            )

            database.child("Admins").child(userId).setValue(userInfo)
                .addOnSuccessListener {
                    Toast.makeText(this, "User info saved to database", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, com.capztone.admin.MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to save user info: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Invalid password. Please enter a valid password.", Toast.LENGTH_SHORT).show()
        }
    }




}
