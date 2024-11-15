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
        auth = FirebaseAuth.getInstance()
        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }
        // Show loading indicator
        binding.progress.visibility = View.VISIBLE



// Start a delay to hide the loading indicator after 1500 milliseconds (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE
            // Call your method to retrieve cart items or perform other operations
        }, 1300)
        // Initialize RecyclerView and Adapter for menu items
        val noProductTextView = findViewById<TextView>(R.id.noProductTextView)
        adapter = SubMenuItemAdapter(this, listOf(), databaseReference, noProductTextView)
        binding.allItemRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.allItemRecyclerView.adapter = adapter

        // Initialize RecyclerView and Adapter for order details
        orderDetailsAdapter = OrderDetailsAdapter()
        binding.allItemRecyclerView1.layoutManager = LinearLayoutManager(this)
        binding.allItemRecyclerView1.adapter = orderDetailsAdapter

        retrieveMenuItem()
        retrieveOrderDetails()
        retrieveShopName()

        // Back button click listener
        binding.BackButton.setOnClickListener {
            finish()
        }

        binding.button.setOnClickListener {
            storeItemsInProducts()
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
                    // Use the shop name in the path for "Products" and "Discount_items"
                    val productsRef = databaseReference.child("Shops").child(shopName).child("Products")


                    // Separate the discount items from other items
                    val allItems = ArrayList<Any>()
                    allItems.addAll(menuItems)
                    allItems.addAll(discountItems)

                    // Update stock status based on quantity and prepare to store items
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
                            }
                            is DiscountItem -> {
                                // Parse the quantity string to extract the numeric value
                                val quantityValue = item.quantitys?.replace("[^0-9]".toRegex(), "")?.toIntOrNull()
                                item.stocks = if (quantityValue == null || quantityValue == 0) {
                                    "Out Of Stock"
                                } else {
                                    "In Stock"
                                }
                            }
                        }
                    }

                    // Debugging: Log the updated items to verify stock status
                    Log.d("StoreItems", "All items before storing: $allItems")

                    // Store non-discount items in the "Products" path
                    productsRef.setValue(allItems)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Items stored in Products successfully", Toast.LENGTH_SHORT).show()
                            // Navigate to Inventory page
                            val intent = Intent(this, SubAdminInventory::class.java)
                            startActivity(intent)
                        }
                        .addOnFailureListener { e ->
                            Log.e("StoreItems", "Failed to store items in Products: $e")
                            Toast.makeText(this, "Failed to store items in Products", Toast.LENGTH_SHORT).show()
                        }



                }
            }.addOnFailureListener { exception ->
                Log.e("StoreItems", "Failed to retrieve shop name: $exception")
                Toast.makeText(this, "Failed to retrieve shop name", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun synchronizeQuantities() {
        for (order in orderDetailsAdapter.getOrders()) {
            // Ensure order.foodNames is a String, then split
            val orderEnglishName = order.foodNames?.toString()?.split("/")?.get(0)?.trim() ?: ""
            val foodQuantities = order.foodQuantities.map { it.toIntOrNull() ?: 0 }

            for ((index, foodQuantity) in foodQuantities.withIndex()) {
                for (menuItem in menuItems) {
                    menuItem.foodName?.let { names ->
                        // Ensure menuItem.foodName is a String, then split
                        val menuItemEnglishName = names.toString().split(",")[0].trim()

                        // Compare the English names
                        if (menuItemEnglishName == orderEnglishName) {
                            val currentQuantity = menuItem.quantity?.toIntOrNull() ?: 0
                            val newQuantity = currentQuantity - foodQuantity

                            if (newQuantity >= 0) {
                                menuItem.quantity = newQuantity.toString()
                                updateQuantityInFirebase(menuItem.key, newQuantity.toString())
                            } else {
                                Log.e("SyncQuantities", "Negative quantity for $menuItemEnglishName")
                            }
                            // Exit loop if match found
                        }
                    }
                }
            }
        }
    }

    private fun updateQuantityInFirebase(menuItemKey: String?, newQuantity: String) {
        menuItemKey?.let { key ->
            val currentUser = FirebaseAuth.getInstance().currentUser
            currentUser?.let { user ->
                val userId = user.uid
                // Reference to the "Delivery Details" path to get the shop name
                val generalAdminRef: DatabaseReference =
                    database.reference.child("Admins").child(userId)

                // Listener to get the shopName
                generalAdminRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        // Retrieve the shop name
                        val shopName = snapshot.child("Shop Id").getValue(String::class.java)
                        shopName?.let { name ->

                            // Reference to the shop's root path
                            val shopRef = database.reference.child("Shops").child(name)

                            // First, check the "discount" path
                            val discountRef = shopRef.child("discount")
                            discountRef.child(key)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(discountSnapshot: DataSnapshot) {
                                        if (discountSnapshot.exists()) {
                                            // If the SKU ID exists in "discount", update the quantity
                                            discountSnapshot.ref.child("quantity")
                                                .setValue(newQuantity)
                                                .addOnSuccessListener {
                                                    Log.d(
                                                        "FirebaseUpdate",
                                                        "Quantity updated successfully in discount"
                                                    )
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e(
                                                        "FirebaseUpdate",
                                                        "Failed to update quantity in discount: $e"
                                                    )
                                                }

                                        } else {
                                            // If not found in discount, check other paths
                                            checkOtherMenuPaths(shopRef, key, newQuantity)
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.e(
                                            "FirebaseUpdate",
                                            "Error fetching discount data: $error"
                                        )
                                    }
                                })
                        }
                    }


                    override fun onCancelled(error: DatabaseError) {
                        // Handle Firebase error
                        Log.e("FirebaseUpdate", "Failed to retrieve shop name: $error")
                    }
                })
            }
        }
    }
    private fun checkOtherMenuPaths(shopRef: DatabaseReference, menuItemKey: String, newQuantity: String) {
        // List to hold other child paths except "Products"
        val childPaths = mutableListOf<DatabaseReference>()

        // Fetch all child paths under the shop except "Products"
        shopRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Loop through all children under the shop path
                for (childSnapshot in dataSnapshot.children) {
                    val childName = childSnapshot.key
                    // Exclude "Products"
                    if (childName != "Products" && childName != "discount" && childName!="Discount-items") {
                        childPaths.add(shopRef.child(childName!!))
                    }
                }

                // Function to search and update quantity once the correct category is found
                fun searchAndUpdateQuantity(menuRef: DatabaseReference, onFound: (Boolean) -> Unit) {
                    menuRef.child(menuItemKey).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                // If the SKU ID exists in this category, update the quantity
                                snapshot.ref.child("quantity").setValue(newQuantity)
                                    .addOnSuccessListener {
                                        Log.d("FirebaseUpdate", "Quantity updated successfully in ${menuRef.key}")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("FirebaseUpdate", "Failed to update quantity: $e")
                                    }
                                onFound(true) // Indicate that the category was found
                            } else {
                                onFound(false) // Continue searching in the next category
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("FirebaseUpdate", "Error fetching data: $error")
                        }
                    })
                }

                // Loop through the filtered categories and update the correct one
                for (menuRef in childPaths) {
                    searchAndUpdateQuantity(menuRef) { found ->
                        if (found) return@searchAndUpdateQuantity // Stop searching once the correct category is found
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle Firebase error
                Log.e("FirebaseUpdate", "Error retrieving child paths: $error")
            }
        })
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
    }

    private fun retrieveOrderDetails() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = auth.currentUser?.uid

            if (userId == null) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                return
            }

            // Reference to the "Admins" node to retrieve the shop name
            val adminRef: DatabaseReference = database.reference.child("Admins").child(userId)

            adminRef.child("Shop name").get().addOnSuccessListener { dataSnapshot ->
                val shopName = dataSnapshot.getValue(String::class.java)
                if (shopName != null) {
                    // If the shop name exists, proceed with retrieving the order details
                    val orderDetailsRef: DatabaseReference = database.reference.child("OrderDetails")

                    orderDetailsRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val orders = mutableListOf<Order>()
                            for (orderSnapshot in dataSnapshot.children) {
                                val shopNamesList = orderSnapshot.child("shopNames").getValue(object : GenericTypeIndicator<ArrayList<String>>() {})
                                val shopIndex = shopNamesList?.indexOf(shopName)
                                if (shopIndex != null && shopIndex != -1) {
                                    val foodNamesSnapshot = orderSnapshot.child("foodNames")
                                    val foodQuantitiesSnapshot = orderSnapshot.child("foodQuantities")

                                    val foodNames: MutableList<String> = mutableListOf()
                                    val foodQuantities: MutableList<String> = mutableListOf()

                                    for (foodNameSnapshot in foodNamesSnapshot.children) {
                                        foodNames.add(foodNameSnapshot.value.toString())
                                    }

                                    for (foodQuantitySnapshot in foodQuantitiesSnapshot.children) {
                                        foodQuantities.add(foodQuantitySnapshot.value.toString())
                                    }

                                    val order = Order(foodNames, foodQuantities)
                                    orders.add(order)
                                }
                            }
                            orderDetailsAdapter.setOrders(orders)
                            synchronizeQuantities() // Synchronize quantities after fetching order details
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.d("DatabaseError", "Error: ${databaseError.message}")
                            Toast.makeText(
                                this@SubAdminAllItemsActivity,
                                "Order Data Not Fetching Error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to retrieve shop name: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
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