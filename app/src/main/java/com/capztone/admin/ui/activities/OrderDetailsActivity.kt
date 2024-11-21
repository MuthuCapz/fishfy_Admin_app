package com.capztone.admin.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.adapters.OrderAdapter
import com.capztone.admin.databinding.ActivityOrderDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailsBinding
    private lateinit var orderDetailsAdapter: OrderAdapter
    private lateinit var orderDetailsReference: DatabaseReference
    private lateinit var orderStatusReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Database references
        orderDetailsReference = FirebaseDatabase.getInstance().getReference("OrderDetails")
        orderStatusReference = FirebaseDatabase.getInstance().getReference("status")

        // Initialize RecyclerView
        binding.orderRecycler.layoutManager = LinearLayoutManager(this)
        orderDetailsAdapter = OrderAdapter(mutableListOf(),binding.noorders,binding.textViewPendingOrdersTitle,binding.pendingorder,binding.textViewCompletedOrders,binding.completedorder)
        binding.orderRecycler.adapter = orderDetailsAdapter


        // Fetch order details from Firebase
        orderDetailsAdapter.fetchOrderDetailsFromFirebase()
        // Finish Activity
        binding.backButton.setOnClickListener {
            finish()
        }
        // Fetch order status from Firebase
        fetchOrderStatuses()
    }

    private fun fetchOrderStatuses() {
        val currentUserId = getCurrentUserId()
        val adminReference = FirebaseDatabase.getInstance().getReference("Delivery Details")

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
                                if (orderId != null && message != null) {
                                    orderDetailsAdapter.updateStatusMessage(orderId, message)
                                }
                            }
                            updateOrderCounts()
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

        binding.completedorder.text = "$deliveredOrderCount"
        binding.pendingorder.text = "$pendingOrders"
    }

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "unknownUserId"
    }

    companion object {
        private const val TAG = "OrderDetailsActivity"
    }
}