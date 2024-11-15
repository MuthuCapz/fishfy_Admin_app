package com.capztone.admin.ui.activities

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.adapters.ViewShopAdapter
import com.capztone.admin.databinding.ActivityViewShopBinding
import com.capztone.admin.databinding.DialogDeleteConfirmationBinding
import com.capztone.admin.databinding.SkuExistingDeleteBinding
import com.capztone.admin.models.ViewShop
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViewShopActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewShopBinding
    private lateinit var shopDatabase: DatabaseReference // For ShopNames path
    private lateinit var deliveryDetailsDatabase: DatabaseReference // For Delivery Details path
    private lateinit var shopList: MutableList<ViewShop>
    private lateinit var filteredShopList: MutableList<ViewShop> // List for filtered shops
    private lateinit var shopSearchAdapter: ViewShopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewShopBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up status bar transparency
        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }

        // Initialize Firebase Database references
        shopDatabase = FirebaseDatabase.getInstance().getReference("ShopNames")
        deliveryDetailsDatabase = FirebaseDatabase.getInstance().getReference("Delivery Details")

        // Initialize lists and adapter
        shopList = mutableListOf()
        filteredShopList = mutableListOf() // Initialize filtered list
        shopSearchAdapter = ViewShopAdapter(this,filteredShopList, { shopId ->
            storeShopIdAndNavigate(shopId)
        }) { shopId ->
            showdeletedialog(shopId)
        }

        binding.shopRecycler.layoutManager = LinearLayoutManager(this)
        binding.shopRecycler.adapter = shopSearchAdapter

        // Fetch shop data from Firebase
        binding.progress.visibility = View.VISIBLE

        fetchShopData()

        // Handle back button click
        binding.searchBackButton.setOnClickListener {
            finish()
        }

    }
    fun skuExistingCartDialog() {
        val dialog = Dialog(this)

        dialog.setCancelable(false)

        // Inflate the custom layout using ViewBinding
        val binding =  SkuExistingDeleteBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)


        // Set up the "No" button action
        binding.btnOk.setOnClickListener {
            dialog.dismiss()   // Just close the dialog without deleting
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }
    private fun showdeletedialog(shopId: String) {

        val dialog = Dialog(this)

        dialog.setCancelable(false)

        // Inflate the custom layout using ViewBinding
        val binding = DialogDeleteConfirmationBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)


        // Set up the "Yes" button action
        binding.btnDialogYes.setOnClickListener {
            deleteShopData(shopId)
            dialog.dismiss()   // Close the dialog
        }

        // Set up the "No" button action
        binding.btnDialogNo.setOnClickListener {
            dialog.dismiss()   // Just close the dialog without deleting
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }



    private fun fetchShopData() {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()) // Define the date format

        shopDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                shopList.clear() // Clear old data
                for (shopSnapshot in snapshot.children) {
                    val shopId = shopSnapshot.key // e.g., SHOP10001, SHOP10002

                    // Retrieve shop details
                    val shopNameSnapshot = shopSnapshot.child("shopName")
                    val mobilenumberSnapshot = shopSnapshot.child("mobileNumber")
                    val createdDateSnapshot = shopSnapshot.child("CreatedDate") // Fetch CreatedDate

                    val shopName = shopNameSnapshot.getValue(String::class.java)
                    val mobileNumber = mobilenumberSnapshot.getValue(String::class.java)
                    val createdDateString = createdDateSnapshot.getValue(String::class.java) // Get CreatedDate as String

                    Log.d("ShopSearchActivity", "Shop ID: $shopId, Shop Name: $shopName, Created Date: $createdDateString")

                    if (shopId != null && shopName != null && createdDateString != null) {
                        try {
                            // Parse the CreatedDate string into a Date object
                            val createdDate: Date = dateFormat.parse(createdDateString) ?: Date(0) // Default to epoch if null

                            // Convert the Date to a timestamp (milliseconds since epoch)
                            val timestamp = createdDate.time

                            // Add the shop to the list with the timestamp
                            shopList.add(ViewShop(shopId, shopName, mobileNumber, timestamp))
                        } catch (e: Exception) {
                            Log.e("ShopSearchActivity", "Error parsing CreatedDate: ${e.message}")
                        }
                    }
                }

                // Sort shops by CreatedDate (timestamp) in descending order (most recent first)
                filteredShopList.clear()
                filteredShopList.addAll(shopList.sortedByDescending { it.createdDate }) // Sort by timestamp
                shopSearchAdapter.notifyDataSetChanged()
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.progress.visibility = View.GONE
                }, 1300) // 2 seconds delay
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ShopSearchActivity", "Database error: ${error.message}")
                // Hide ProgressBar if error occurs
                binding.progress.visibility = View.GONE
            }
        })
    }


    private fun deleteShopData(shopId: Any) {
        val shopIdString = shopId.toString() // Ensure shopId is a String

        // Delete from ShopLocations, Shops, and ShopNames
        val pathsToDelete = listOf("ShopLocations", "Shops", "ShopNames","Categories")
        for (path in pathsToDelete) {
            FirebaseDatabase.getInstance().getReference(path).child(shopIdString).removeValue()
                .addOnSuccessListener {
                    Log.d("DeleteShopData", "Deleted shopId $shopIdString from $path")
                }.addOnFailureListener { e ->
                    Log.e("DeleteShopData", "Failed to delete from $path: ${e.message}")
                }
        }

        // Reference to the "Addresses" path
        val locationsRef = FirebaseDatabase.getInstance().getReference("Addresses")

// Iterate through all children of "Addresses"
        locationsRef.get().addOnSuccessListener { snapshot ->
            for (child in snapshot.children) {
                // Get the "Shop Id" value for each child
                val shopIdsString = child.child("Shop Id").value as? String

                // Check if "Shop Id" exists and contains the ID to delete
                if (!shopIdsString.isNullOrEmpty()) {
                    // Split the "Shop Id" string into a list
                    val shopIdsList = shopIdsString.split(",").map { it.trim() }

                    // Check if the ID exists in the list
                    if (shopIdsList.contains(shopIdString)) {
                        // Remove the specific Shop Id
                        val updatedShopIds = shopIdsList.filter { it != shopIdString }.joinToString(", ")

                        // Update the "Shop Id" field with the modified list
                        child.ref.child("Shop Id").setValue(updatedShopIds)
                            .addOnSuccessListener {
                                Log.d("DeleteShopData", "Deleted Shop Id from ${child.key} path")
                            }
                            .addOnFailureListener { e ->
                                Log.e("DeleteShopData", "Failed to delete Shop Id: ${e.message}")
                            }
                    }
                }
            }
        }

        val adminsRef = FirebaseDatabase.getInstance().getReference("Admins")

// Iterate through each user under "Admins"
        adminsRef.get().addOnSuccessListener { snapshot ->
            for (child in snapshot.children) {
                val shopIdValue = child.child("Shop Id").value as? String
                if (shopIdValue == shopIdString) {
                    // Delete the entire user entry (identified by user ID)
                    child.ref.removeValue()
                        .addOnSuccessListener {
                            Log.d("DeleteShopData", "Deleted user entry from Admins path: ${child.key}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("DeleteShopData", "Failed to delete user entry: ${e.message}")
                        }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("DeleteShopData", "Failed to retrieve Admins: ${e.message}")
        }

    }



    private fun storeShopIdAndNavigate(shopId: String) {
        // Store the selected shopId in Delivery Details path
        deliveryDetailsDatabase.child("Shop Id").setValue(shopId)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ShopSearchActivity", "Shop Id $shopId stored successfully.")
                    // Navigate to MainActivity after storing the shopId
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Optionally finish the current activity
                } else {
                    Log.e("ShopSearchActivity", "Failed to store Shop ID: ${task.exception?.message}")
                }
            }
    }
}