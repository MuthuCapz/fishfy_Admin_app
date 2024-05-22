package com.capztone.admin.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.databinding.ActivityAllItemsBinding
import com.capztone.admin.models.AllMenu
import com.capztone.admin.adapters.MenuItemAdapter
import com.capztone.admin.adapters.OrderDetailsAdapter
import com.capztone.admin.models.DiscountItem
import com.capztone.admin.models.Order
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class AllItemsActivity : AppCompatActivity(), MenuItemAdapter.OnItemClicked {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var menuItems: ArrayList<AllMenu> = ArrayList()
    private var menu1Items: ArrayList<DiscountItem> = ArrayList()
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: MenuItemAdapter

    private lateinit var orderDetailsAdapter: OrderDetailsAdapter

    private val binding: ActivityAllItemsBinding by lazy {
        ActivityAllItemsBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        databaseReference = database.reference

        auth = FirebaseAuth.getInstance()
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
        // Initialize RecyclerView and Adapter


        // Initialize RecyclerView and Adapter for order details
        orderDetailsAdapter = OrderDetailsAdapter()
        binding.allItemRecyclerView1.layoutManager = LinearLayoutManager(this)
        binding.allItemRecyclerView1.adapter = orderDetailsAdapter

        retrieveMenuItem()
        retrieveOrderDetails()



        // Back button click listener
        binding.BackButton.setOnClickListener {
            finish()
        }
        binding.button.setOnClickListener {
            updateProducts()
        }
    }


    private fun synchronizeQuantities() {

        for (order in orderDetailsAdapter.getOrders()) {
            val foodNames = order.foodNames
            val foodQuantities = order.foodQuantities.map { it.toIntOrNull() ?: 0 }

            for ((index, foodName) in foodNames.withIndex()) {
                for (menuItem in menuItems) {
                    if (menuItem.foodName == foodName) {
                        val currentQuantity = menuItem.quantity?.toIntOrNull() ?: 0
                        val newQuantity = currentQuantity - foodQuantities[index]
                        if (newQuantity >= 0) {
                            menuItem.quantity = newQuantity.toString()
                            updateQuantityInFirebase(menuItem.key, newQuantity.toString())
                        } else {
                            Log.e("SyncQuantities", "Negative quantity for ${menuItem.foodName}")
                        }
                        break // Assuming each food name is unique, exit loop if match found
                    }
                }
            }
        }
    }


    private fun updateQuantityInFirebase(menuItemKey: String?, newQuantity: String) {
        menuItemKey?.let { key ->
            val path = when {
                key.startsWith("menu") -> "menu"
                key.startsWith("menu1") -> "menu1"
                key.startsWith("menu2") -> "menu2"
                key.startsWith("discount") -> "discount"
                else -> null
            }
            path?.let { p ->
                database.reference
                    .child(p)
                    .child(key)
                    .child("quantity")
                    .setValue(newQuantity)
                    .addOnSuccessListener {
                        Log.d("FirebaseUpdate", "Quantity updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirebaseUpdate", "Failed to update quantity: $e")
                    }
            }
        }
    }

    override fun updateProducts() {
        // Get the current user's admin ID
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

        // Reference to the Firebase database
        val databaseReference: DatabaseReference = database.reference

        // List to store menu items belonging to the current user
        val userMenuItems = menuItems.filter { it.adminId == currentUserID }
        val userMenuItems1 = menu1Items.filter { it.adminId == currentUserID }

        // Reference to the path where the products will be stored
        val productsRef = currentUserID?.let { databaseReference.child("products").child(it) }

        // Reference to the path where the discount items will be stored under the current user's ID
        val discountItemsRef = currentUserID?.let { databaseReference.child("Discount-items").child(it) }

        // Update regular menu items
        for (menuItem in userMenuItems) {
            productsRef?.child(menuItem.key ?: "")?.setValue(menuItem)
                ?.addOnSuccessListener {
                    Toast.makeText(this, "Products added to Firebase successfully", Toast.LENGTH_SHORT).show()
                    Log.d("FirebaseUpdate", "Product updated successfully")
                }
                ?.addOnFailureListener { e ->
                    Log.e("FirebaseUpdate", "Failed to update product: $e")
                    Toast.makeText(this, "Failed to update product", Toast.LENGTH_SHORT).show()
                }
        }

        // Update menu1 items
        for (menu1Item in userMenuItems1) {
            discountItemsRef?.child(menu1Item.key ?: "")?.setValue(menu1Item)
                ?.addOnSuccessListener {
                    Toast.makeText(this, "Products added to Firebase successfully", Toast.LENGTH_SHORT).show()
                    Log.d("FirebaseUpdate", "Product updated successfully")
                }
                ?.addOnFailureListener { e ->
                    Log.e("FirebaseUpdate", "Failed to update product: $e")
                    Toast.makeText(this, "Failed to update product", Toast.LENGTH_SHORT).show()
                }
        }

        // Update discount items

        }




    private fun retrieveMenuItem() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid

            // Reference to the "Admins" path
            val adminRef: DatabaseReference = database.reference.child("Admins").child(userId)

            // Listener to get the shopName
            val adminListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Retrieve the shopName
                    val shopName = snapshot.child("shopName").getValue(String::class.java)
                    shopName?.let { name ->
                        // Now that we have the shopName, we can set up the menu references
                        val menuRef: DatabaseReference = database.reference.child(name).child("menu")
                        val menu1Ref: DatabaseReference = database.reference.child(name).child("menu1")
                        val menu2Ref: DatabaseReference = database.reference.child(name).child("menu2")
                        val menu3Ref: DatabaseReference = database.reference.child(name).child("discount")

                        // Listener for menu
                        val menuListener = object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                // Handle menu data here
                                val itemList = ArrayList<AllMenu>()
                                for (foodSnapshot in snapshot.children) {
                                    val menuItem = foodSnapshot.getValue(AllMenu::class.java)
                                    menuItem?.let {
                                        if (it.adminId == userId) {
                                            itemList.add(it)
                                        }
                                    }
                                }
                                menuItems.addAll(itemList)
                                setAdapter(menuItems + menu1Items)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle cancellation for menu
                            }
                        }
                        val menu1Listener = object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                // Handle menu1 data here
                                val itemList = ArrayList<DiscountItem>()
                                for (foodSnapshot in snapshot.children) {
                                    val menuItem = foodSnapshot.getValue(DiscountItem::class.java)
                                    menuItem?.let {
                                        if (it.adminId == userId) {
                                            itemList.add(it)
                                        }
                                    }
                                }
                                menu1Items.addAll(itemList)
                                setAdapter(menuItems + menu1Items)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle cancellation for menu1
                            }
                        }

                        // Fetch data from menu path
                        menuRef.addListenerForSingleValueEvent(menuListener)
                        // Fetch data from menu1 path
                        menu1Ref.addListenerForSingleValueEvent(menuListener)
                        // Fetch data from menu2 path
                        menu2Ref.addListenerForSingleValueEvent(menuListener)
                        menu3Ref.addListenerForSingleValueEvent(menu1Listener)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle cancellation for admin
                }
            }

            // Fetch the shopName from the "Admins" path
            adminRef.addListenerForSingleValueEvent(adminListener)
        }
    }

    private fun setAdapter(items: List<Any>) {
        val adapter = MenuItemAdapter(this@AllItemsActivity, items, databaseReference)
        binding.allItemRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.allItemRecyclerView.adapter = adapter
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

            adminRef.child("shopName").get().addOnSuccessListener { dataSnapshot ->
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
                                this@AllItemsActivity,
                                "Order Data Not Fetching Error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                } else {
                    Toast.makeText(this, "Shop name not found", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to retrieve shop name: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteItem(skuId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Retrieve the current user's shop name from the database
        userId?.let { uid ->
            val userRef = databaseReference.child("Admins").child(uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val shopName = dataSnapshot.child("shopName").value.toString()

                    // Use the retrieved shop name to construct the paths for deletion
                    val menuRef = databaseReference.child(shopName).child("menu").child(skuId)
                    val menu1Ref = databaseReference.child(shopName).child("menu1").child(skuId)
                    val menu2Ref = databaseReference.child(shopName).child("menu2").child(skuId)
                    val menu3Ref = databaseReference.child(shopName).child("discount").child(skuId)

                    val deleteOperations = listOf(
                        menuRef.removeValue(),
                        menu1Ref.removeValue(),
                        menu2Ref.removeValue(),
                        menu3Ref.removeValue()
                    )

                    // Execute all delete operations together
                    Tasks.whenAll(deleteOperations)
                        .addOnSuccessListener {
                            Toast.makeText(this@AllItemsActivity, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.e("DeleteItem", "Failed to delete item: $e")
                            Toast.makeText(this@AllItemsActivity, "Failed to delete item", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })
        }
    }


}


