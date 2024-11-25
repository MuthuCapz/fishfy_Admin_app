package com.capztone.admin.ui.activities

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Adapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivitySubAdminAllItemsBinding
import com.capztone.admin.models.AllMenu
import com.capztone.admin.adapters.MenuItemAdapter
import com.capztone.admin.adapters.OrderDetailsAdapter
import com.capztone.admin.adapters.SubMenuItemAdapter
import com.capztone.admin.databinding.DialogDeleteConfirmationBinding
import com.capztone.admin.databinding.SkuExistingDeleteBinding
import com.capztone.admin.models.DiscountItem
import com.capztone.admin.models.Order
import com.capztone.admin.models.RetrieveItem
import com.capztone.admin.utils.FirebaseAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubAdminAllItemsActivity: AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var menuItems: ArrayList<AllMenu> = ArrayList()
    private var menu1Items: ArrayList<AllMenu> = ArrayList() // Fixed type to AllMenu
    private var discountItems: ArrayList<DiscountItem> = ArrayList()
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: SubMenuItemAdapter
    private lateinit var orderDetailsAdapter: OrderDetailsAdapter
    private val processedItems = mutableSetOf<String>()

    private val binding: ActivitySubAdminAllItemsBinding by lazy {
        ActivitySubAdminAllItemsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference
        auth = FirebaseAuthUtil.auth

        // Show loading indicator
        binding.progress.visibility = View.VISIBLE



// Start a delay to hide the loading indicator after 1500 milliseconds (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE
            // Call your method to retrieve cart items or perform other operations
        }, 1300)
        // Initialize RecyclerView and Adapter for menu items
        val noProductTextView = findViewById<TextView>(R.id.noProductTextView)
        val button = findViewById<Button>(R.id.button)
        adapter = SubMenuItemAdapter(this, listOf(), databaseReference, noProductTextView, button)
        binding.allItemRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.allItemRecyclerView.adapter = adapter

        // Initialize RecyclerView and Adapter for order details


        retrieveMenuItem()
        retrieveShopName()

        // Back button click listener
        binding.BackButton.setOnClickListener {
            finish()
        }

        binding.button.setOnClickListener {
            val intent = Intent(this, SubAdminInventory::class.java)
            startActivity(intent)
        }
    }
    private fun storeItemsInProducts() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid

            // Retrieve the shop name from the "Admins" node
            val generalAdminRef: DatabaseReference = databaseReference.child("Admins").child(userId)
            generalAdminRef.child("Shop Id").get().addOnSuccessListener { snapshot ->
                val shopName = snapshot.getValue(String::class.java)
                if (shopName != null) {
                    // Separate the discount items from other items
                    val allItems = ArrayList<Any>()
                    allItems.addAll(menuItems)
                    allItems.addAll(discountItems)

                    // Update stock status based on quantity and SKU ID
                    allItems.forEach { item ->
                        when (item) {
                            is AllMenu -> {
                                // Parse the quantity string to extract the numeric value
                                val quantityValue = item.quantity?.replace("[^0-9]".toRegex(), "")?.toIntOrNull()
                                item.stock = if (quantityValue == null || quantityValue == 0) {
                                    "Out Of Stock"
                                } else {
                                    "In Stock"
                                }

                                // Check SKU ID in each category inside "Shops" for stock update
                                val shopRef = databaseReference.child("Shops").child(shopName)
                                shopRef.get().addOnSuccessListener { shopSnapshot ->
                                    if (shopSnapshot.exists()) {
                                        // Loop through all categories in the shop
                                        for (categorySnapshot in shopSnapshot.children) {
                                            // Fetch SKU reference inside category
                                            val skuRef = categorySnapshot.child(item.key ?: "")
                                            if (skuRef.exists()) {
                                                // Update stock status based on SKU ID and quantity
                                                val skuDatabaseRef = databaseReference
                                                    .child("Shops")
                                                    .child(shopName)
                                                    .child(categorySnapshot.key ?: "")
                                                    .child(item.key ?: "")

                                                if (quantityValue == null || quantityValue == 0) {
                                                    skuDatabaseRef.child("stock").setValue("Out Of Stock")
                                                } else {
                                                    skuDatabaseRef.child("stock").setValue("In Stock")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is DiscountItem -> {
                                // Parse the quantity string to extract the numeric value
                                val quantityValue = item.quantitys?.replace("[^0-9]".toRegex(), "")?.toIntOrNull()
                                item.stocks = if (quantityValue == null || quantityValue == 0) {
                                    "Out Of Stock"
                                } else {
                                    "In Stock"
                                }

                                // Check SKU ID in each category inside "Shops" for stock update
                                val shopRef = databaseReference.child("Shops").child(shopName)
                                shopRef.get().addOnSuccessListener { shopSnapshot ->
                                    if (shopSnapshot.exists()) {
                                        // Loop through all categories in the shop
                                        for (categorySnapshot in shopSnapshot.children) {
                                            // Fetch SKU reference inside category
                                            val skuRef = categorySnapshot.child(item.key ?: "")
                                            if (skuRef.exists()) {
                                                // Update stock status based on SKU ID and quantity
                                                val skuDatabaseRef = databaseReference
                                                    .child("Shops")
                                                    .child(shopName)
                                                    .child(categorySnapshot.key ?: "")
                                                    .child(item.key ?: "")

                                                if (quantityValue == null || quantityValue == 0) {
                                                    skuDatabaseRef.child("stocks").setValue("Out Of Stock")
                                                } else {
                                                    skuDatabaseRef.child("stocks").setValue("In Stock")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }.addOnFailureListener { exception ->
                Log.e("StoreItems", "Failed to retrieve shop name: $exception")
                Toast.makeText(this, "Failed to retrieve shop name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun retrieveShopName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val generalAdminRef: DatabaseReference = database.reference.child("Admins").child(userId)

            val generalAdminListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shopName = snapshot.child("Shop Id").getValue(String::class.java)
                    shopName?.let { name ->
                        binding.ShopName.text = shopName
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle cancellation for general admin
                }
            }

            generalAdminRef.addListenerForSingleValueEvent(generalAdminListener)
        }
    }

    private fun retrieveMenuItem() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = auth.currentUser?.uid

            // Reference to the "General Admin" path
            val generalAdminRef: DatabaseReference? =
                userId?.let { database.reference.child("Admins").child(it) }

            // Listener to get the shopName
            val generalAdminListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Retrieve the shopName
                    val shopName = snapshot.child("Shop Id").getValue(String::class.java)
                    shopName?.let { name ->
                        // Reference to the shop name's path
                        val shopRef = database.reference.child("Shops").child(name)

                        // Listener to get all child paths except "Products" and "discount"
                        shopRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                val menuRefs = mutableListOf<DatabaseReference>()

                                for (childSnapshot in dataSnapshot.children) {
                                    val childKey = childSnapshot.key
                                    if (childKey != "Products" && childKey != "discount" && childKey!="Discount-items") {
                                        // Add all paths except "Products" and "discount"
                                        childKey?.let {
                                            menuRefs.add(shopRef.child(it))
                                        }
                                    }
                                }

                                // Add the "discount" path separately if it exists
                                if (dataSnapshot.hasChild("discount")) {
                                    menuRefs.add(shopRef.child("discount"))
                                }

                                // Method to handle data retrieval from menu paths
                                fun fetchMenuItems(menuRef: DatabaseReference, itemList: MutableList<AllMenu>, onItemsLoaded: () -> Unit) {
                                    menuRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            for (foodSnapshot in snapshot.children) {
                                                val menuItem = foodSnapshot.getValue(AllMenu::class.java)
                                                menuItem?.let {
                                                    // Retrieve all food names (not just the 0th position)
                                                    it.foodName?.let { foodNamesList ->
                                                        if (foodNamesList.isNotEmpty()) {
                                                            // Join food names into a comma-separated string
                                                            val foodNamesCommaSeparated = foodNamesList.joinToString(", ")
                                                            it.foodName = arrayListOf(foodNamesCommaSeparated) // Store comma-separated string
                                                            itemList.add(it)
                                                        }
                                                    }
                                                }
                                            }
                                            onItemsLoaded()
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Handle Firebase error
                                        }
                                    })
                                }

                                // Fetch all menu items
                                var loadedMenus = 0
                                val allMenuItems = mutableListOf<AllMenu>()
                                menuRefs.forEach { menuRef ->
                                    fetchMenuItems(menuRef, allMenuItems) {
                                        loadedMenus++
                                        if (loadedMenus == menuRefs.size) {
                                            menuItems.clear()
                                            menuItems.addAll(allMenuItems)
                                            updateAdapter() // Update after all menus are loaded
                                        }
                                    }
                                }

                                // Fetch discount items if "discount" was included
                                val discountRef = shopRef.child("discount")
                                if (dataSnapshot.hasChild("discount")) {
                                    discountRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            val discountList = mutableListOf<DiscountItem>()
                                            for (foodSnapshot in snapshot.children) {
                                                val discountItem = foodSnapshot.getValue(DiscountItem::class.java)
                                                discountItem?.let {
                                                    // Retrieve all food names for discount items
                                                    it.foodNames?.let { foodNamesList ->
                                                        if (foodNamesList.isNotEmpty()) {
                                                            // Join food names into a comma-separated string
                                                            val foodNamesCommaSeparated = foodNamesList.joinToString(", ")
                                                            it.foodNames = arrayListOf(foodNamesCommaSeparated) // Store comma-separated string
                                                            discountList.add(it)
                                                        }
                                                    }
                                                }
                                            }
                                            discountItems.clear()
                                            discountItems.addAll(discountList)
                                            updateAdapter()
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Handle Firebase error
                                        }
                                    })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle Firebase error
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle Firebase error
                }
            }

            // Fetch the shopName from the "General Admin" path
            if (generalAdminRef != null) {
                generalAdminRef.addListenerForSingleValueEvent(generalAdminListener)
            }
        }
    }


    private fun updateAdapter() {
        val allItems = mutableListOf<Any>()
        allItems.addAll(menuItems)
        allItems.addAll(menu1Items)
        allItems.addAll(discountItems)
        adapter.updateItems(allItems)
        storeItemsInProducts()
    }

    fun  showDeleteConfirmationDialog(skuId: String) {
        val dialog = Dialog(this)

        dialog.setCancelable(false)

        // Inflate the custom layout using ViewBinding
        val binding = DialogDeleteConfirmationBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)



        // Set up the "Yes" button action
        binding.btnDialogYes.setOnClickListener {
            deleteItem(skuId)  // Call the delete function
            dialog.dismiss()   // Close the dialog
        }

        // Set up the "No" button action
        binding.btnDialogNo.setOnClickListener {
            dialog.dismiss()   // Just close the dialog without deleting
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
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
    fun deleteItem(skuId: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid
        val userEmail = user?.email

        userId?.let { uid ->
            val userRef = databaseReference.child("Admins").child(userId)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val shopName = dataSnapshot.child("Shop Id").value.toString()
                    val shopRef = databaseReference.child("Shops").child(shopName)

                    // List to hold references for deletion
                    val deleteOperations = mutableListOf<DatabaseReference>()

                    // Get all child paths under the shop, excluding "Products" and "Discount-items"
                    shopRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(shopSnapshot: DataSnapshot) {
                            for (childSnapshot in shopSnapshot.children) {
                                val childName = childSnapshot.key
                                // Exclude "Products" and "Discount-items"
                                if (childName != "Products" && childName != "Discount-items") {
                                    val itemRef = shopRef.child(childName!!).child(skuId)
                                    deleteOperations.add(itemRef)

                                    // Store details for each deleted item with skuId
                                    val itemDetails = childSnapshot.child(skuId).value as? Map<String, Any>
                                    itemDetails?.let {
                                        storeDeletedItem(userEmail, shopName, skuId, itemDetails)
                                    }
                                }
                            }

                            // Execute delete operations
                            for (ref in deleteOperations) {
                                ref.removeValue()
                                    .addOnSuccessListener {
                                        Log.d("DeleteItem", "Item deleted successfully")
                                        Toast.makeText(this@SubAdminAllItemsActivity, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                                        retrieveMenuItem()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("DeleteItem", "Failed to delete item: $e")
                                        Toast.makeText(this@SubAdminAllItemsActivity, "Failed to delete item", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("DeleteItem", "Database error: ${databaseError.message}")
                        }
                    })
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("DeleteItem", "Error retrieving shop name: ${databaseError.message}")
                }
            })
        }
    }

    // Function to store each deleted item under its SKU ID in the "DeletedItems" path
    private fun storeDeletedItem(userEmail: String?, shopName: String, skuId: String, itemDetails: Map<String, Any>) {
        val deletedItemsRef = databaseReference.child("DeletedItems").child(skuId)

        // Get current date and time in a readable format
        val currentTime = getCurrentDateTime()
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val Username = account?.displayName ?: "Unknown User"
        // Merge shopName into the existing itemDetails
        val updatedItemDetails = itemDetails.toMutableMap().apply {
            put("shopName", shopName)
            put("userEmail", userEmail ?: "")
            put("DeletedDated", currentTime) // Add the timestamp to the item details
            put("DeletedBy", Username)
        }

        // Store the merged details under the skuId
        deletedItemsRef.setValue(updatedItemDetails)
            .addOnSuccessListener {
                Log.d("DeleteItem", "Deleted item details for SKU ID: $skuId saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e("DeleteItem", "Failed to save deleted item details for SKU ID: $skuId - $e")
            }
    }

    // Function to get current date and time in "dd-MM-yyyy hh:mm a" format
    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
        return dateFormat.format(Date())
    }



}