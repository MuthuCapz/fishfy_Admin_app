package com.capztone.admin.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import com.capztone.admin.R
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.adapters.SubOrderAdapter
import com.capztone.admin.databinding.ActivitySubAdminOrderdetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SubAdminOrderdetails: AppCompatActivity() {

    private lateinit var binding: ActivitySubAdminOrderdetailsBinding
    private lateinit var orderDetailsAdapter: SubOrderAdapter
    private lateinit var orderDetailsReference: DatabaseReference
    private lateinit var orderStatusReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubAdminOrderdetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database references
        orderDetailsReference = FirebaseDatabase.getInstance().getReference("OrderDetails")
        orderStatusReference = FirebaseDatabase.getInstance().getReference("status")
        val passedText = intent.getStringExtra("orderText")

        // Initialize RecyclerView
        binding.orderRecycler.layoutManager = LinearLayoutManager(this)
        orderDetailsAdapter = SubOrderAdapter(mutableListOf(),binding.noorders, passedText ?: "")
        binding.orderRecycler.adapter = orderDetailsAdapter


        // Finish Activity
        binding.backButton.setOnClickListener {
            finish()
        }
        // Retrieve the passed text from the Intent

        // Find the textView16 by its ID
        val textView16 = findViewById<TextView>(R.id.textView16)

        // Set the retrieved text to textView16
        textView16.text = passedText

        // Fetch order details from Firebase
        orderDetailsAdapter.fetchOrderDetailsFromFirebase()

        // Fetch order status from Firebase
        fetchOrderStatuses()
    }
    private fun fetchOrderStatuses() {
        val currentUserId = getCurrentUserId()
        val adminReference = FirebaseDatabase.getInstance().getReference("Admins").child(currentUserId)

        adminReference.child("Shop Id").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shopName = snapshot.getValue(String::class.java)

                if (shopName != null) {
                    val deliveryReference = FirebaseDatabase.getInstance().getReference("$shopName delivery")
                    deliveryReference.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            for (statusSnapshot in snapshot.children) {
                                val message = statusSnapshot.child("message").getValue(String::class.java)
                                val orderId = statusSnapshot.key
                                Log.d(TAG, "Order ID: $orderId, Message: $message") // Debugging log

                                // Check if the message is "Order delivered" before updating the adapter
                                if (orderId != null && message == "Order delivered") {
                                    orderDetailsAdapter.updateStatusMessage(orderId, message)
                                }
                            }
                            updateOrderCounts() // Update order counts after processing
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Error fetching delivery statuses: ${error.message}")
                        }
                    })
                } else {
                    Log.e(TAG, "Shop name not found for current user")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching shop name: ${error.message}")
            }
        })
    }

    private fun updateOrderCounts() {
        val deliveredOrderCount = orderDetailsAdapter.getDeliveredOrderCount()
        val totalOrders = orderDetailsAdapter.itemCount
        val pendingOrders = totalOrders - deliveredOrderCount

    }

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "unknownUserId"
    }

    companion object {
        private const val TAG = "OrderDetailsActivity"
    }
}