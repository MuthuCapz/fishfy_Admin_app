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
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivitySubAdminsPasswordBinding
import com.capztone.admin.models.Shop
import com.capztone.admin.utils.FirebaseAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubAdminsPassword : AppCompatActivity() {

    private lateinit var binding: ActivitySubAdminsPasswordBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var shopList: MutableList<Shop> = mutableListOf()
    private lateinit var selectedShop: Shop

    data class Shop(
        var shopId: String = "",  // Add shopId here
        var password: String = "",
        var shopName: String = "",
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
    ) {
        constructor() : this("", "","", 0.0, 0.0)
    }


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubAdminsPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

auth = FirebaseAuthUtil.auth
        database = FirebaseDatabase.getInstance().reference
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val Username = account?.displayName ?: "Unknown User"

        val email = intent.getStringExtra("EMAIL")
        val lastLoggedInEmail = intent.getStringExtra("lastLoggedInEmail")
        binding.textViewEmail.text = lastLoggedInEmail ?: "Email: $email"
        val passwordEditText = findViewById<EditText>(R.id.editTextPassword)
        val togglePasswordVisibility = findViewById<ImageView>(R.id.imageViewTogglePassword)

        var isPasswordVisible = false

        togglePasswordVisibility.setOnClickListener {
            if (isPasswordVisible) {
                // Hide Password
                passwordEditText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                togglePasswordVisibility.setImageResource(R.drawable.invisible)
                isPasswordVisible = false
            } else {
                // Show Password
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT
                togglePasswordVisibility.setImageResource(R.drawable.visible)
                isPasswordVisible = true
            }
            // Move cursor to the end of the text
            passwordEditText.setSelection(passwordEditText.text.length)
        }
        // Set retrieved data to TextViews
        binding.textViewUsername.text = "Username: $Username"
        binding.textViewEmail.text = "Email: $email"




        val shopNameTextView: TextView = findViewById(R.id.selectShop)

// Get the current user's email from Firebase Authentication
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        if (currentUserEmail != null) {
            // Reference to Firebase database path
            val databaseReference = FirebaseDatabase.getInstance().getReference("ShopNames")

            // Retrieve data from Firebase
            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var shopNameFound: String? = null

                    // Loop through Shop IDs
                    for (shopSnapshot in snapshot.children) {
                        val shopMailId =
                            shopSnapshot.child("shopMailId").getValue(String::class.java)
                        val shopName = shopSnapshot.child("shopName").getValue(String::class.java)

                        // Check if shopMailId matches the logged-in user's email
                        if (shopMailId == currentUserEmail) {
                            shopNameFound = shopName
                            break // Exit the loop if a match is found
                        }
                    }

                    // Display the shop name if found
                    if (shopNameFound != null) {
                        shopNameTextView.text = shopNameFound
                    } else {
                        shopNameTextView.text = "Shop name not found"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle any errors
                    shopNameTextView.text = "Failed to load data"
                }
            })
        } else {
            // If no user is logged in, show appropriate message
            shopNameTextView.text = "User not logged in"
        }

        if (currentUserEmail != null) {
            val databaseReference = FirebaseDatabase.getInstance().getReference("ShopNames")

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var ownerNameFound: String? = null
                    var addressFound: String? = null
                    var mobileNumberFound: String? = null

                    for (shopSnapshot in snapshot.children) {
                        val shopMailId =
                            shopSnapshot.child("shopMailId").getValue(String::class.java)

                        // Check if shopMailId matches the logged-in user's email
                        if (shopMailId == currentUserEmail) {
                            ownerNameFound =
                                shopSnapshot.child("ownerName").getValue(String::class.java)
                            addressFound =
                                shopSnapshot.child("address").getValue(String::class.java)
                            mobileNumberFound =
                                shopSnapshot.child("mobileNumber").getValue(String::class.java)
                            break
                        }
                    }

                    // Display the retrieved details if a match was found
                    if (ownerNameFound != null && addressFound != null && mobileNumberFound != null) {
                        binding.shopOwnerName.setText(ownerNameFound)
                        binding.address.setText(addressFound)
                        binding.mobileNumber.setText(mobileNumberFound)
                    } else {
                        binding.shopOwnerName.setText("Owner name not found")
                        binding.address.setText("Address not found")
                        binding.mobileNumber.setText("Mobile number not found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.shopOwnerName.setText("Failed to load data")
                    binding.address.setText("Failed to load data")
                    binding.mobileNumber.setText("Failed to load data")
                }
            })
        } else {
            binding.shopOwnerName.setText("User not logged in")
            binding.address.setText("User not logged in")
            binding.mobileNumber.setText("User not logged in")
        }

        fetchShopNamesFromFirebase()

        binding.btnSubmit.setOnClickListener {
            val password = binding.editTextPassword.text.toString()
            val address = binding.address.text.toString()
            val shopName = shopNameTextView.text.toString() // Get the displayed shop name
            val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)


            // Save user verification status
            sharedPreferences.edit().putBoolean("isUserVerified", true).apply()



            // Find the shop in shopList using the displayed shop name
            selectedShop = shopList.find { it.shopName == shopName }!!

            if (selectedShop != null) {
                val shopId = selectedShop.shopId
                val profileImageUrl = account?.photoUrl.toString()
                val ownerName = binding.shopOwnerName.text.toString()
                val address = binding.address.text.toString()
                val mobileNumber = binding.mobileNumber.text.toString()

                // Validation checks
                if (ownerName.isBlank() || ownerName.length > 15) {
                    Toast.makeText(
                        this,
                        "Name cannot be blank and must be within 15 characters.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("Validation", "Invalid owner name: $ownerName")
                    return@setOnClickListener
                }

                if (address.isBlank() || address.length > 150) {
                    Toast.makeText(
                        this,
                        "Address cannot be blank and must not exceed 150 characters.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("Validation", "Invalid address: $address")
                    return@setOnClickListener
                }

                if (mobileNumber.isBlank() || !mobileNumber.matches("\\d{10}".toRegex())) {
                    Toast.makeText(
                        this,
                        "Mobile number must be exactly 10 digits.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("Validation", "Invalid mobile number: $mobileNumber")
                    return@setOnClickListener
                }

                // Save shop details after validation
                saveShopDetails(shopId, profileImageUrl, ownerName, address, mobileNumber)

                val shopRef = database.child("ShopNames").child(shopId)
                shopRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val shopMailId = snapshot.child("shopMailId").value as? String
                        val shopPassword = snapshot.child("password").value as? String

                        if (shopMailId == email && shopPassword == password && address.isNotBlank()) {
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                            if (email != null) {
                                saveUserData(Username, password, shopId, address, email)
                            }
                            val intent = Intent(this, SubAdminMainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Login to shop mail or enter correct password.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(this, "Shop not found.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Error retrieving shop data: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this, "Please select a valid shop.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Function to save shop details under ShopNames path in Firebase
    private fun saveShopDetails(shopId: String, profileImageUrl: String, ownerName: String, address: String, mobileNumber: String) {
        val shopDetails = mapOf(
            "profileImage" to profileImageUrl,
            "ownerName" to ownerName,
            "address" to address,
            "mobileNumber" to mobileNumber
        )

        database.child("ShopNames").child(shopId).updateChildren(shopDetails)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                } else {
                    Toast.makeText(this, "Failed to save shop details!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun fetchShopNamesFromFirebase() {
        database.child("ShopNames").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                shopList.clear()  // Clear the shop list to avoid duplicates
                for (shopSnapshot in dataSnapshot.children) {
                    val shopId = shopSnapshot.key ?: ""  // Get the shopId (key)
                    val shop = shopSnapshot.getValue(Shop::class.java)?.let {
                        Shop(shopId, it.password, it.shopName)  // Create Shop instance with shopId
                    }
                    if (shop != null) {
                        shopList.add(shop)  // Add to the shop list
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
            }
        })
    }




    private fun saveUserData(
        Username: String?,
        password: String,
        shopId: String,
        address: String,
        email: String
    ) {
        val geocoder = Geocoder(this)
        try {
            val addressList = geocoder.getFromLocationName(address, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val location = addressList[0]
                val latitude = location.latitude
                val longitude = location.longitude
                val locality = location.locality ?: "Unknown Locality"

                val currentDate = Date()
                val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
                val updatedDate = dateFormat.format(currentDate)
                val userId = FirebaseAuth.getInstance().currentUser?.uid

                if (userId != null) {
                    val shopUserRef = database.child("Admins").child(userId)

                    shopUserRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val existingEmail = snapshot.child("email").value as? String
                            val existingPassword = snapshot.child("password").value as? String
                            val existingShopId = snapshot.child("Shop Id").value as? String

                            if (existingEmail == email && existingPassword == password && existingShopId == shopId) {
                                val updateData = mapOf(
                                    "UpdatedBy" to Username,
                                    "UpdatedDate" to updatedDate
                                )

                                shopUserRef.updateChildren(updateData)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Toast.makeText(
                                                this,
                                                "User data updated successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                this,
                                                "Failed to update data!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Please enter the correct shop email and password!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val createdDate = dateFormat.format(currentDate)
                            val userData = mapOf(
                                "CreatedBy" to Username,
                                "email" to email,
                                "CreatedDate" to createdDate,
                                "Shop Id" to shopId
                            )

                            shopUserRef.setValue(userData).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(
                                        this,
                                        "User data saved successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(this, "Failed to save data!", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to retrieve user data!", Toast.LENGTH_SHORT)
                            .show()
                    }

                    val shopLocationRef = database.child("ShopLocations").child(shopId)

                    shopLocationRef.get().addOnSuccessListener { locationSnapshot ->
                        if (locationSnapshot.exists()) {
                            val updateLocationData = mapOf(
                                "latitude" to latitude,
                                "longitude" to longitude,
                                "locality" to locality,
                                "UpdatedDate" to updatedDate,
                                "UpdatedBy" to Username
                            )

                            shopLocationRef.updateChildren(updateLocationData)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(
                                            this,
                                            "Shop location updated successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Failed to update shop location!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            val createdDate = dateFormat.format(currentDate)
                            val newLocationData = mapOf(
                                "latitude" to latitude,
                                "longitude" to longitude,
                                "locality" to locality,
                                "CreatedDate" to createdDate,
                                "CreatedBy" to Username
                            )

                            shopLocationRef.setValue(newLocationData)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Toast.makeText(
                                            this,
                                            "Shop location saved successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Failed to save shop location!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                    }.addOnFailureListener {
                        Toast.makeText(
                            this,
                            "Failed to retrieve shop location data!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Address not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error converting address to latlng", Toast.LENGTH_SHORT).show()
        }
    }

}