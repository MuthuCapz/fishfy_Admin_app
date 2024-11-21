package com.capztone.admin.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivityPasswordBinding
import com.capztone.admin.ui.activities.GeneralAdmin
import com.capztone.admin.utils.FirebaseAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class PasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswordBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var allowedPassword: String? = null

    @SuppressLint("SuspiciousIndentation")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

auth = FirebaseAuthUtil.auth
        database = FirebaseDatabase.getInstance().reference



        // Try to retrieve data from the intent
        val username = intent.getStringExtra("USERNAME") ?: getUsernameFromPreferences()
        val email= intent.getStringExtra("EMAIL") ?: getEmailFromPreferences()
        // Set retrieved data to TextViews
        binding.textViewUsername.text = "Username: $username"
        binding.textViewEmail.text = "Email: $email"
        // Fetch allowed password from Firebase
        fetchAllowedPassword()
        // Disable the button initially
        binding.btnSubmit.isEnabled = false
        var isPasswordVisible = false

        binding.imageViewTogglePassword.setOnClickListener {
            if (isPasswordVisible) {
                // Hide Password
                binding.editTextPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.imageViewTogglePassword.setImageResource(R.drawable.invisible)
                isPasswordVisible = false
            } else {
                // Show Password
                binding.editTextPassword.inputType = InputType.TYPE_CLASS_TEXT
                binding.imageViewTogglePassword.setImageResource(R.drawable.visible)
                isPasswordVisible = true
            }
            // Move cursor to the end of the text
            binding.editTextPassword.setSelection(binding.editTextPassword.text.length)
        }

        // Add text change listeners to both password and address fields
        binding.editTextPassword.addTextChangedListener(passwordTextWatcher)


        binding.btnSubmit.setOnClickListener {
            val password = binding.editTextPassword.text.toString()


            // Check if the password is valid
            if (password.isNotBlank()) {
                saveUserInfo(password)

            } else {
                Toast.makeText(this, "Please enter password", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun fetchAllowedPassword() {
        // Firebase path to retrieve password
        val passwordRef = database.child("General Admin").child("Password")
        passwordRef.get().addOnSuccessListener { snapshot ->
            allowedPassword = snapshot.getValue(String::class.java)
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to retrieve password: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getUsernameFromPreferences(): String {
        val sharedPreferences = getSharedPreferences("user_info", MODE_PRIVATE)
        return sharedPreferences.getString("USERNAME", null) ?: "Unknown"
    }

    private fun getEmailFromPreferences(): String {
        val sharedPreferences = getSharedPreferences("user_info", MODE_PRIVATE)
        return sharedPreferences.getString("EMAIL", null) ?: "Unknown"
    }
    // TextWatcher for password field
    private val passwordTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Enable the button only if both password and address fields are filled
            binding.btnSubmit.isEnabled = s.toString().isNotEmpty()
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

    private fun saveUserInfo(password: String) {
        val user = auth.currentUser
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val Username = account?.displayName ?: "Unknown User"
        val email = intent.getStringExtra("EMAIL") ?: ""


        if (password == allowedPassword) {
            val userId = user?.uid ?: ""

            // Get current date and time
            val currentDate = Date()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
            val currentFormattedDate = dateFormat.format(currentDate)

            // Reference to the user in the database
            val userRef = database.child("General Admin")

            userRef.get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    // User data exists, update the necessary fields
                    val updateData = hashMapOf(
                        "UpdatedBy" to Username,
                        "UpdatedDate" to currentFormattedDate
                    )

                    userRef.updateChildren(updateData as Map<String, Any>).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "User info updated successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainAddNewShopActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to update user info: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // User data does not exist, create new data with CreatedDate and CreatedBy
                    val userInfo = hashMapOf(


                        "CreatedBy" to Username,
                        "CreatedDate" to currentFormattedDate
                    )

                    userRef.setValue(userInfo)
                        .addOnSuccessListener {
                            Toast.makeText(this, "User info saved to database", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainAddNewShopActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user info: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to retrieve user info: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Invalid password. Please enter a valid password.", Toast.LENGTH_SHORT).show()
        }
    }




}