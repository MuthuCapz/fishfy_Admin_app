package com.capztone.admin.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.PopupMenu

import java.io.InputStreamReader
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.adapters.InventoryAdapter
import com.capztone.admin.databinding.ActivityInventoryBinding
import com.capztone.admin.models.RetrieveItem
import com.capztone.admin.R
import com.capztone.admin.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.IOException
import android.content.ContentValues
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.capztone.admin.adapters.SubInventoryAdapter
import com.capztone.admin.databinding.ActivitySubAdminInventoryBinding
import com.capztone.admin.utils.FirebaseAuthUtil

import java.io.OutputStream

class SubAdminInventory : AppCompatActivity() {

    private lateinit var binding: ActivitySubAdminInventoryBinding
    private lateinit var adapter: SubInventoryAdapter
    private lateinit var databaseReference: DatabaseReference
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private lateinit var auth: FirebaseAuth
    private var shopName: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubAdminInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
auth = FirebaseAuthUtil.auth

        // Fetch the shop name for the current user
        fetchShopName()



        binding.Addmore.setOnClickListener {
            saveDataToFirebase()
        }

        // Show loading indicator
        binding.progress.visibility = View.VISIBLE



// Start a delay to hide the loading indicator after 1500 milliseconds (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE
            // Call your method to retrieve cart items or perform other operations
        }, 900)
        // Finish Activity
        binding.backButton.setOnClickListener {
            finish()
        }
        // Initialize RecyclerView and adapter with callback
        adapter = SubInventoryAdapter(this) { hasData ->
            // Show NoProduct text view if no data is available
            binding.NoProduct.visibility = if (hasData) View.GONE else View.VISIBLE
        }
        // Initialize RecyclerView and adapter

        binding.recyclerView1.layoutManager = LinearLayoutManager(this)
        binding.recyclerView1.adapter = adapter

        // Fetch data from Firebase and populate RecyclerView
        adapter.fetchData()
    }

    private fun fetchShopName() {
        val currentUserID = auth.currentUser?.uid
        if (currentUserID != null) {
            val adminRef = database.child("Admins").child(currentUserID)
            adminRef.child("Shop Id").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    shopName = snapshot.getValue(String::class.java)
                    binding.ShopName.text=shopName
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        applicationContext,
                        "Failed to retrieve shop name: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }




    private fun saveDataToFirebase() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        currentUserID?.let { userId ->
            // Step 1: Retrieve the Shop ID from the "Delivery Details" path
            database.child("Admins").child(currentUserID).child("Shop Id").get().addOnSuccessListener { shopIdSnapshot ->
                val shopId = shopIdSnapshot.getValue(String::class.java) // This should give you the Shop ID

                shopId?.let { id ->
                    // Step 2: Get the updated items and discount items from the adapter
                    val items = adapter.getItems()
                    val discountItems = adapter.getDiscountItems()  // Retrieve discount items

                    // Step 3: Save the updated items to Firebase under the correct path
                    for (item in items) {
                        val key = item.key
                        key?.let {
                            database.child("Shops").child(id).child("Inventory").child(it).setValue(item)
                                .addOnSuccessListener {
                                    // Data saved successfully
                                    Toast.makeText(this, "Updated Inventory successfully", Toast.LENGTH_SHORT).show()
                                    exportDataToCSV()
                                }
                                .addOnFailureListener {
                                    // Failed to save data
                                    Toast.makeText(this, "Failed to update Inventory", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }

                    // Step 4: Save the discount items to Firebase under the correct path
                    for (discountItem in discountItems) {
                        val discountKey = discountItem.key
                        discountKey?.let {
                            database.child("Shops").child(id).child("Inventory").child(it).setValue(discountItem)
                                .addOnSuccessListener {
                                    // Discount data saved successfully
                                    Toast.makeText(this, "Updated Discounts successfully", Toast.LENGTH_SHORT).show()
                                    exportDataToCSV()
                                }
                                .addOnFailureListener {
                                    // Failed to save discount data
                                    Toast.makeText(this, "Failed to update Discounts", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }

                } ?: run {
                    // Shop ID is null or empty
                    Toast.makeText(this, "Shop ID not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                // Failed to retrieve Shop ID
                Toast.makeText(this, "Failed to retrieve Shop ID", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            // User ID is null
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportDataToCSV() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserID != null) {
            // Fetch the shop name
            database.child("Admins").child(currentUserID).child("Shop Id").get().addOnSuccessListener { dataSnapshot ->
                val shopName = dataSnapshot.getValue(String::class.java)
                if (shopName != null) {
                    // Create references for the shop's inventory and discounts using the shop name
                    val inventoryRef = database.child("Shops").child(shopName).child("Inventory")
                    val discountsRef = database.child("Shops").child(shopName).child("Discount-items")

                    // Fetch and export inventory and discount data
                    inventoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(inventorySnapshot: DataSnapshot) {
                            discountsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                @RequiresApi(Build.VERSION_CODES.Q)
                                override fun onDataChange(discountsSnapshot: DataSnapshot) {
                                    try {
                                        val fileName = "$shopName-Inventory.csv"  // Include shop name in the file name
                                        val mimeType = "text/csv"
                                        val contentValues = ContentValues().apply {
                                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                        }

                                        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                        uri?.let {
                                            val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                                            outputStream?.use { stream ->
                                                val csvWriter = stream.writer()
                                                csvWriter.append("Food Name English,Food Name Tamil,Food Name Malayalam, Food Name Telugu, SKU ID,Food Price,Image Url,Category,Stock,Quantity\n")

                                                // Write inventory items to CSV
                                                for (itemSnapshot in inventorySnapshot.children) {
                                                    val item = itemSnapshot.getValue(RetrieveItem::class.java)
                                                    item?.let {
                                                        val line =
                                                            "${it.foodName},${it.key},${it.foodPrice},${it.foodImage},${it.category},${it.stock},${it.quantity}\n"
                                                        csvWriter.append(line)
                                                    }
                                                }

                                                // Write discounts items to CSV
                                                for (itemSnapshot in discountsSnapshot.children) {
                                                    val item = itemSnapshot.getValue(DiscountItem::class.java)
                                                    item?.let {
                                                        val line =
                                                            "${it.foodNames},${it.key},${it.foodPrices},${it.foodImages},${it.categorys},${it.stocks},${it.quantitys}\n"
                                                        csvWriter.append(line)
                                                    }
                                                }

                                                csvWriter.flush()
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Inventory exported successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        } ?: run {
                                            throw IOException("Failed to create new MediaStore record.")
                                        }
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                        Toast.makeText(
                                            applicationContext,
                                            "Error exporting inventory: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Toast.makeText(
                                        applicationContext,
                                        "Failed to export discounts: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            })
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                applicationContext,
                                "Failed to export inventory: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "Shop name not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to retrieve Shop name", Toast.LENGTH_SHORT).show()
            }
        }
    }
}