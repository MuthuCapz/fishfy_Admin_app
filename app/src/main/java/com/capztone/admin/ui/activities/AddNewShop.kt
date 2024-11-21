package com.capztone.admin.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.databinding.ActivityAddNewShopBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.capztone.admin.R
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class AddNewShop : AppCompatActivity() {

    private lateinit var binding: ActivityAddNewShopBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddNewShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up password visibility toggle
        var isPasswordVisible = false
        binding.passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            binding.password.inputType = if (isPasswordVisible) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            binding.passwordToggle.setImageResource(if (isPasswordVisible) R.drawable.visible else R.drawable.invisible)

        }

        binding.backButton.setOnClickListener {
            finish()
        }

        // Handle Save Shop button click
        binding.btnSubmit.setOnClickListener {
            val shopName = binding.newshopname.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val shopMail = binding.emailTextView.text.toString().trim()
            val mobile = binding.mobileTextView.text.toString().trim()
            val ownerName = binding.ownerTextView.text.toString().trim()
            val city = binding.cityTextView.text.toString().trim()

            if (shopName.isNotEmpty() && password.isNotEmpty() && shopMail.isNotEmpty() && mobile.isNotEmpty() && ownerName.isNotEmpty() && city.isNotEmpty()) {
                checkPasswordInFirebase(shopName, shopMail, password, mobile, ownerName, city)
            } else {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkPasswordInFirebase(shopName: String, shopMail: String, password: String, mobile: String, ownerName: String, city: String) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("ShopNames")

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var passwordExists = false
                var emailExists = false
                for (shopSnapshot in dataSnapshot.children) {
                    val storedPassword = shopSnapshot.child("password").getValue(String::class.java)
                    val storedMail = shopSnapshot.child("shopMailId").getValue(String::class.java)
                    if (storedPassword == password) {
                        passwordExists = true
                    }
                    if (storedMail == shopMail) {
                        emailExists = true
                    }
                    // Exit loop early if both conditions are met
                    if (passwordExists && emailExists) break
                }

                when {
                    passwordExists -> {
                        Toast.makeText(applicationContext, "Try another password, this one already exists", Toast.LENGTH_LONG).show()
                    }
                    emailExists -> {
                        Toast.makeText(applicationContext, "Try another email, this one already exists", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        saveShopNameToFirebase(shopName, shopMail, password, mobile, ownerName, city)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext, "Error: ${databaseError.message}", Toast.LENGTH_LONG).show()
            }
        })
    }


    private fun saveShopNameToFirebase(shopName: String, shopMail: String, password: String, mobile: String, ownerName: String, city: String) {
        val shopRef = FirebaseDatabase.getInstance().getReference("ShopNames")
        shopRef.orderByKey().limitToLast(1).get().addOnSuccessListener { dataSnapshot ->
            var lastShopId = "SHOP10000"
            val account = GoogleSignIn.getLastSignedInAccount(this)
            val username = account?.displayName ?: "Unknown User"

            if (dataSnapshot.children.any()) {
                val lastShopKey = dataSnapshot.children.first().key
                lastShopId = lastShopKey ?: "SHOP10000"
            }

            val newShopId = incrementShopId(lastShopId)
            val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
            val createdDate = dateFormat.format(Date())

            val shopData = mapOf(
                "shopName" to shopName,
                "shopMailId" to shopMail,
                "password" to password,
                "mobileNumber" to mobile,
                "ownerName" to ownerName,
                "address" to city,
                "CreatedDate" to createdDate,
                "CreatedBy" to username
            )

            // Save data under "ShopNames"
            shopRef.child(newShopId).setValue(shopData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Shop added with ID $newShopId", Toast.LENGTH_SHORT).show()

                    // Also store the shop ID under "Delivery Details"
                    val intent = Intent(this, GeneralAdmin::class.java).apply {
                        putExtra("shopName", shopName)
                    }
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add shop: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to retrieve last shop ID: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun incrementShopId(lastShopId: String): String {
        val numericId = lastShopId.removePrefix("SHOP").toIntOrNull() ?: 10000
        return "SHOP${numericId + 1}"
    }
}