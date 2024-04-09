package com.example.admin.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.admin.databinding.ActivityAllItemsBinding
import com.example.admin.models.AllMenu
import com.example.admin.adapters.MenuItemAdapter
import com.example.admin.adapters.OrderDetailsAdapter
import com.example.admin.models.Order
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.*

class AllItemsActivity : AppCompatActivity(), MenuItemAdapter.OnItemClicked {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var menuItems: ArrayList<AllMenu> = ArrayList()

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
        // Assuming you have a reference to the Firebase "products" path
        val productsRef = database.reference.child("products")

        // Iterate through menuItems and update products in the Firebase database
        for (menuItem in menuItems) {
            productsRef.child(menuItem.key ?: "").setValue(menuItem)
                .addOnSuccessListener {
                    Toast.makeText(this, "Products added firebase successfully", Toast.LENGTH_SHORT)
                        .show()
                    Log.d("FirebaseUpdate", "Product updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseUpdate", "Failed to update product: $e")
                }
        }
    }


    private fun retrieveMenuItem() {
        val menuRef: DatabaseReference = database.reference.child("menu")
        val menu1Ref: DatabaseReference = database.reference.child("menu1")
        val menu2Ref: DatabaseReference = database.reference.child("menu2")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemList = ArrayList<AllMenu>()
                for (foodSnapshot in snapshot.children) {
                    val menuItem = foodSnapshot.getValue(AllMenu::class.java)
                    menuItem?.let {
                        itemList.add(it)
                    }
                }
                menuItems.addAll(itemList)
                setAdapter()
                synchronizeQuantities() // Synchronize quantities after fetching menu items
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError", "Error: ${error.message}")
                Toast.makeText(this@AllItemsActivity, "Data Not Fetching Error", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Fetch data from menu path
        menuRef.addListenerForSingleValueEvent(listener)
        // Fetch data from menu1 path
        menu1Ref.addListenerForSingleValueEvent(listener)
        // Fetch data from menu2 path
        menu2Ref.addListenerForSingleValueEvent(listener)
    }

    private fun retrieveOrderDetails() {
        val orderDetailsRef: DatabaseReference = database.reference.child("OrderDetails")

        orderDetailsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val orders = mutableListOf<Order>()
                for (orderSnapshot in dataSnapshot.children) {
                    val foodNames: MutableList<String> = mutableListOf()
                    val foodQuantities: MutableList<String> = mutableListOf()

                    for (foodNameSnapshot in orderSnapshot.child("foodNames").children) {
                        foodNames.add(foodNameSnapshot.value.toString())
                    }

                    for (foodQuantitySnapshot in orderSnapshot.child("foodQuantities").children) {
                        foodQuantities.add(foodQuantitySnapshot.value.toString())
                    }

                    val order = Order(foodNames, foodQuantities)
                    orders.add(order)
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
    }

    private fun setAdapter() {
        val adapter = MenuItemAdapter(this@AllItemsActivity, menuItems, databaseReference)
        binding.allItemRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.allItemRecyclerView.adapter = adapter
    }

    fun deleteItem(skuId: String) {
        val menuRef = databaseReference.child("menu").child(skuId)
        val menu1Ref = databaseReference.child("menu1").child(skuId)
        val menu2Ref = databaseReference.child("menu2").child(skuId)

        val deleteOperations = listOf(
            menuRef.removeValue(),
            menu1Ref.removeValue(),
            menu2Ref.removeValue()
        )

        // Execute all delete operations together
        Tasks.whenAll(deleteOperations)
            .addOnSuccessListener {
                Toast.makeText(this, "Item deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("DeleteItem", "Failed to delete item: $e")
                Toast.makeText(this, "Failed to delete item", Toast.LENGTH_SHORT).show()
            }
    }


}