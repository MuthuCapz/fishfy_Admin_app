package com.capztone.admin.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivityCustomerAccountDetailsBinding
import com.capztone.admin.models.Customer
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CustomerAccountDetails : AppCompatActivity() {

    // Declare ViewBinding variable
    private lateinit var binding: ActivityCustomerAccountDetailsBinding

    // Declare Firebase database reference
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Initialize ViewBinding
        binding = ActivityCustomerAccountDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve userId from Intent
        val userId = intent.getStringExtra("USER_ID")
        val userUid = intent.getStringExtra("USER_UID")
        binding.userId.text = userUid

        // Use userId as needed, for example, setting it in a TextView or fetching customer details
        userId?.let {
            binding.authid.text = it // Set the authId in TextView

            // Initialize Firebase Database reference
            database = FirebaseDatabase.getInstance().getReference("Addresses")

            // Fetch user details from Firebase
            getUserDetails(it)

        }
    }

    private fun getUserDetails(authId: String) {
        // Path to the user details in Firebase for mobile number and username
        val userRef = database.child(authId).child("User Details")
        // Path to the 'users' reference to check if authId exists
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        // Path to the Addresses data
        val addressesRef = database.child(authId)

        // Fetch the data from Firebase
        userRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Get mobile number and username
                val mobileNumber = snapshot.child("mobile number").value.toString()
                val userName = snapshot.child("user name").value.toString()
                binding.HOME.text="HOME"
                binding.work.text="WORK"
                binding.other.text="OTHER"
                // Display the retrieved data
                binding.mobileNumber.text = mobileNumber
                binding.userName.text = userName
            } else {
                Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            // Handle any errors
            Toast.makeText(this, "Error retrieving user details", Toast.LENGTH_SHORT).show()
        }
// Fetch the profile image from 'users' if the authId exists
        usersRef.child(authId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val profileImageLink = snapshot.child("profileImage").value?.toString() ?: ""
                if (profileImageLink.isNotEmpty()) {
                    // Use Glide to load the profile image
                    Glide.with(this)
                        .load(profileImageLink)
                        .into(binding.userImage) // Assuming you have an ImageView with this ID in your layout
                } else {
                    // Set default image if profileImageLink is empty
                    binding.userImage.setImageResource(R.drawable.pic_default) // Replace 'default_image' with your default drawable
                }
            } else {
                // Set default image if snapshot doesn't exist
                binding.userImage.setImageResource(R.drawable.pic_default)
            }
        }.addOnFailureListener {
            // Handle any errors fetching the profile image
            Toast.makeText(this, "Error retrieving profile image", Toast.LENGTH_SHORT).show()
            // Set default image in case of error
            binding.userImage.setImageResource(R.drawable.pic_default)
        }


        // Fetch the address data from the 'Addresses' path
        addressesRef.child("HOME").get().addOnSuccessListener { homeSnapshot ->
            if (homeSnapshot.exists()) {
                val homeAddress = homeSnapshot.child("address").value.toString()
                binding.address.text = "$homeAddress"
            } else {
                binding.HOME.visibility=View.GONE
                binding.Address.visibility=View.GONE
            }
        }.addOnFailureListener {
            // Handle any errors fetching HOME address
            Toast.makeText(this, "Error retrieving HOME address", Toast.LENGTH_SHORT).show()
        }

        addressesRef.child("WORK").get().addOnSuccessListener { workSnapshot ->
            if (workSnapshot.exists()) {
                val workAddress = workSnapshot.child("address").value.toString()
                binding.addressvalue.text = "$workAddress"
            } else {
                binding.addressvalue.visibility=View.GONE
                binding.work.visibility=View.GONE
            }
        }.addOnFailureListener {
            // Handle any errors fetching WORK address
            Toast.makeText(this, "Error retrieving WORK address", Toast.LENGTH_SHORT).show()
        }

        addressesRef.child("OTHER").get().addOnSuccessListener { otherSnapshot ->
            if (otherSnapshot.exists()) {
                val otherAddress = otherSnapshot.child("address").value.toString()
                binding.address3.text = "$otherAddress"
            } else {
                binding.address3.visibility=View.GONE
                binding.other.visibility=View.GONE
            }
        }.addOnFailureListener {
            // Handle any errors fetching OTHER address
            Toast.makeText(this, "Error retrieving OTHER address", Toast.LENGTH_SHORT).show()
        }
    }
}

