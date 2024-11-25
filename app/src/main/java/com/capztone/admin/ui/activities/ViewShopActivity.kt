package com.capztone.admin.ui.activities

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import android.Manifest
import android.content.ContentValues

import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.adapters.ViewShopAdapter
import com.capztone.admin.databinding.ActivityViewShopBinding
import com.capztone.admin.databinding.DialogDeleteConfirmationBinding
import com.capztone.admin.databinding.SkuExistingDeleteBinding
import com.capztone.admin.models.ViewShop
import com.google.firebase.database.*
import java.io.OutputStream
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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewShopBinding.inflate(layoutInflater)
        setContentView(binding.root)


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

        binding.exportButton.setOnClickListener {
            exportShopListToCSV()
        }


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
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun exportShopListToCSV() {
        try {
            // Prepare CSV content
            val csvHeader = "Shop ID,Shop Name,Mobile Number,Created Date\n"
            val csvData = StringBuilder(csvHeader)

            for (shop in filteredShopList) {
                val formattedDate = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date(shop.createdDate))
                csvData.append("${shop.shopId},${shop.shopName},${shop.mobileNumber ?: "N/A"},${formattedDate}\n")
            }

            // Use MediaStore to save in Downloads directory
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "ShopList.csv") // File name
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv") // File type
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS) // Directory
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    writeCSVToStream(csvData.toString(), outputStream)
                }
                showToast("CSV file exported successfully to Downloads.")
            } else {
                showToast("Failed to export CSV file.")
            }
        } catch (e: Exception) {
            Log.e("ExportCSV", "Error exporting CSV: ${e.message}")
            showToast("Error exporting CSV file.")
        }
    }

    // Helper function to write CSV data to the output stream
    private fun writeCSVToStream(data: String, outputStream: OutputStream) {
        outputStream.write(data.toByteArray())
        outputStream.flush()
    }

    // Helper function to show toast messages
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissions(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return writePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_WRITE_PERMISSION
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            exportShopListToCSV()
        } else {
            showToast("Permission denied. Unable to export CSV.")
        }
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
                        // Remove the specific Shop Id from the list
                        val updatedShopIds = shopIdsList.filter { it != shopIdString }.joinToString(", ")

                        // Update the "Shop Id" field with the modified list
                        child.ref.child("Shop Id").setValue(updatedShopIds)
                            .addOnSuccessListener {
                                Log.d("DeleteShopData", "Deleted Shop Id $shopIdString from ${child.key} path in Addresses")
                            }
                            .addOnFailureListener { e ->
                                Log.e("DeleteShopData", "Failed to delete Shop Id $shopIdString from Addresses: ${e.message}")
                            }
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("DeleteShopData", "Failed to retrieve Addresses: ${e.message}")
        }
        // Reference to the "Delivery Details" path
        val deliveryDetailsRef = FirebaseDatabase.getInstance().getReference("Delivery Details")

        // Check for shopId match and delete the entry only if shopId matches
        deliveryDetailsRef.get().addOnSuccessListener { snapshot ->
            for (child in snapshot.children) {
                val shopIdInDetails = child.key // The shopId in Delivery Details is the key of each child
                if (shopIdInDetails == shopIdString) {
                    // Delete the specific shopId entry from Delivery Details
                    child.ref.removeValue()
                        .addOnSuccessListener {
                            Log.d("DeleteShopData", "Deleted shopId $shopIdString from Delivery Details")
                        }
                        .addOnFailureListener { e ->
                            Log.e("DeleteShopData", "Failed to delete shopId $shopIdString from Delivery Details: ${e.message}")
                        }
                }
            }
        }.addOnFailureListener { e ->
            Log.e("DeleteShopData", "Failed to retrieve Delivery Details: ${e.message}")
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
    companion object {
        private const val REQUEST_WRITE_PERMISSION = 100
    }
}