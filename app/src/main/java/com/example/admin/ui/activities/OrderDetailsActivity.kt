package com.example.admin.ui.activities

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.admin.adapters.OrderAdapter
import com.example.admin.databinding.ActivityOrderDetailsBinding
import com.example.admin.models.OrderDetails
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
        orderDetailsAdapter = OrderAdapter(emptyList())
        binding.orderRecycler.adapter = orderDetailsAdapter

        // Fetch order details from Firebase
        fetchOrderDetailsFromFirebase()

        // Fetch order status from Firebase
        fetchOrderStatusFromFirebase()
        fetchOrderStatusFromFirebases()
    }

    private fun fetchOrderDetailsFromFirebase() {
        orderDetailsReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orderDetailsList = mutableListOf<OrderDetails>()
                for (orderSnapshot in snapshot.children) {
                    try {
                        val orderDetails = orderSnapshot.getValue(OrderDetails::class.java)
                        orderDetails?.let {
                            orderDetailsList.add(it)
                        }
                    } catch (e: DatabaseException) {
                        Log.e(TAG, "Error converting order details: ${e.message}")
                    }
                }
                orderDetailsAdapter.setData(orderDetailsList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching order details: ${error.message}")
            }
        })
    }

    private fun fetchOrderStatusFromFirebase() {
        orderStatusReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var deliveredCount = 0
                for (statusSnapshot in snapshot.children) {
                    try {
                        val message = statusSnapshot.child("message").getValue(String::class.java)
                        if (message == "Order delivered") {
                            deliveredCount++
                        }
                    } catch (e: DatabaseException) {
                        Log.e(TAG, "Error converting order status: ${e.message}")
                    }
                }
                binding.completedorder.text = "$deliveredCount"
                val remainingOrders = orderDetailsAdapter.itemCount - deliveredCount
                binding.pendingorder.text = "$remainingOrders"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching order status: ${error.message}")
            }
        })
    }
    private fun fetchOrderStatusFromFirebases() {
        orderStatusReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (statusSnapshot in snapshot.children) {
                    try {
                        val message = statusSnapshot.child("message").getValue(String::class.java)
                        val orderId = statusSnapshot.key

                        orderId?.let { orderId ->
                            message?.let { message ->
                                orderDetailsAdapter.updateStatusMessage(orderId, message)
                            }
                        }
                    } catch (e: DatabaseException) {
                        Log.e(TAG, "Error converting order status: ${e.message}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error fetching order status: ${error.message}")
            }
        })
    }

    companion object {
        private const val TAG = "OrderDetailsActivity"
    }
}
