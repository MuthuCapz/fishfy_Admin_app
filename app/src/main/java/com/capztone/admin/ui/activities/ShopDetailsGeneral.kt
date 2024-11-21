package com.capztone.admin.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivityShopDetailsGeneralBinding
import com.google.firebase.database.*

class ShopDetailsGeneral : AppCompatActivity() {

    private lateinit var binding: ActivityShopDetailsGeneralBinding
    private lateinit var database: DatabaseReference
    private var shopId: String? = null // Initialize this with the actual shop ID as needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopDetailsGeneralBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference
        setupEditIcons()
        // Retrieve and display shop details
        getShopIdAndDetails()

        // Update button click listener
        binding.updateInformationButton.setOnClickListener {
            updateShopDetails()
        }
        // Back button click listener
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupEditIcons() {
        binding.apply {
            editOwnerNameIcon.setOnClickListener {
                ownerNameTextView.isEnabled = true
                ownerNameTextView.requestFocus()
            }



            editEmailIcon.setOnClickListener {
                emailTextView.isEnabled = true
                emailTextView.requestFocus()
            }

            editPhoneNumberIcon.setOnClickListener {
                phoneNumberTextView.isEnabled = true
                phoneNumberTextView.requestFocus()
            }

            editLocationIcon.setOnClickListener {
                locationTextView.isEnabled = true
                locationTextView.requestFocus()
            }
        }
    }


    private fun getShopIdAndDetails() {
        val deliveryDetailsRef = database.child("Delivery Details").child("Shop Id")

        deliveryDetailsRef.get().addOnSuccessListener { snapshot ->
            shopId = snapshot.value.toString()

            if (shopId != null) {
                val shopRef = database.child("ShopNames").child(shopId!!)

                shopRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        binding.apply {
                            ownerNameTextView.text = Editable.Factory.getInstance().newEditable(dataSnapshot.child("ownerName").value?.toString() ?: "")
                            shopNameTextView.text = Editable.Factory.getInstance().newEditable(dataSnapshot.child("shopName").value?.toString() ?: "")
                            emailTextView.text = Editable.Factory.getInstance().newEditable(dataSnapshot.child("shopMailId").value?.toString() ?: "")
                            phoneNumberTextView.text = Editable.Factory.getInstance().newEditable(dataSnapshot.child("mobileNumber").value?.toString() ?: "")
                            locationTextView.text = Editable.Factory.getInstance().newEditable(dataSnapshot.child("address").value?.toString() ?: "")
                            // Load profile image using Glide

                            // Load profile image using Glide
                            val profileImageUrl = dataSnapshot.child("profileImage").value.toString()
                            Glide.with(this@ShopDetailsGeneral)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.profile) // Replace with a placeholder image
                                .into(profileImageView)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(this@ShopDetailsGeneral, "Failed to load data.", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this@ShopDetailsGeneral, "Shop ID not found.", Toast.LENGTH_SHORT).show()
            }
        }
    }



    // Update shop details in Firebase
    private fun updateShopDetails() {
        val updatedData = mapOf(
            "ownerName" to binding.ownerNameTextView.text.toString(),
            "shopName" to binding.shopNameTextView.text.toString(),
            "shopMailId" to binding.emailTextView.text.toString(),
            "mobileNumber" to binding.phoneNumberTextView.text.toString(),
            "address" to binding.locationTextView.text.toString()
        )

        if (shopId != null) {
            database.child("ShopNames").child(shopId!!).updateChildren(updatedData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Information updated successfully", Toast.LENGTH_SHORT).show()
                    disableEditTexts()
                } else {
                    Toast.makeText(this, "Update failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Shop ID is not available.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun disableEditTexts() {
        binding.apply {
            ownerNameTextView.isEnabled = false
            shopNameTextView.isEnabled = false
            emailTextView.isEnabled = false
            phoneNumberTextView.isEnabled = false
            locationTextView.isEnabled = false
        }
    }
}