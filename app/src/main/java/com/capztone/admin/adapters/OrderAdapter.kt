package com.capztone.admin.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capztone.admin.databinding.ItemTotalOrdersBinding
import com.capztone.admin.models.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrderAdapter(private var orderDetailsList: MutableList<OrderDetails>, private val noOrdersTextView: TextView, private val textViewPendingOrdersTitle: TextView, private val pendingorder: TextView, private val textViewCompletedOrders: TextView, private val completedorder: TextView) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    private val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("OrderDetails")
    private val statusMessagesMap = HashMap<String, String>()
    private var shopName: String? = null
    private var deliveredOrderCount: Int = 0

    init {
        fetchShopName()
    }

    private fun fetchShopName() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val adminRef = FirebaseDatabase.getInstance().getReference("Delivery Details")
            adminRef.child("Shop Id").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    shopName = snapshot.getValue(String::class.java)
                    fetchOrderDetailsFromFirebase()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }

    fun setData(newData: List<OrderDetails>) {
        orderDetailsList = newData.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTotalOrdersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    fun updateStatusMessage(orderId: String, message: String) {
        statusMessagesMap[orderId] = message
        if (message == "Order delivered") {
            deliveredOrderCount++
        }
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val orderDetails = orderDetailsList[position]
        holder.bind(orderDetails)
    }

    override fun getItemCount(): Int {
        // If the order list is empty, show the "No Orders" TextView
        if (orderDetailsList.isEmpty()) {
            noOrdersTextView.visibility = View.VISIBLE
            textViewPendingOrdersTitle.visibility = View.GONE
            pendingorder.visibility = View.GONE
            textViewCompletedOrders.visibility = View.GONE
            completedorder.visibility = View.GONE

        } else {
            noOrdersTextView.visibility = View.GONE
            textViewPendingOrdersTitle.visibility = View.VISIBLE
            pendingorder.visibility = View.VISIBLE
            textViewCompletedOrders.visibility = View.VISIBLE
            completedorder.visibility = View.VISIBLE
        }
        return orderDetailsList.size
    }

    fun fetchOrderDetailsFromFirebase() {
        if (shopName == null) return

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                orderDetailsList.clear()
                deliveredOrderCount = 0
                for (snapshot in dataSnapshot.children) {
                    val orderDetails = snapshot.getValue(OrderDetails::class.java)
                    if (orderDetails != null && shopName in orderDetails.shopNames) {
                        orderDetailsList.add(orderDetails)
                        val message = statusMessagesMap[orderDetails.itemPushKey]
                        if (message == "Order delivered") {
                            deliveredOrderCount++
                        }
                    }
                }
                orderDetailsList.sortByDescending { it.orderDate }
                notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    fun getDeliveredOrderCount(): Int {
        return deliveredOrderCount
    }

    inner class ViewHolder(private val binding: ItemTotalOrdersBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(orderDetails: OrderDetails) {
            binding.itemPushKey.text = "${orderDetails.itemPushKey ?: "N/A"}"

            binding.foodNames.text = "${orderDetails.foodNames?.joinToString(",") ?: "N/A"}"
            binding.foodQuantities.text = "${orderDetails.skuUnitQuantities?.joinToString(",") ?: "N/A"}"
            binding.foodPrices.text = "${orderDetails.foodPrices?.joinToString(",") ?: "N/A"}"
            binding.slot.text = "${orderDetails.selectedSlot}"

            // Extract and display the address without the first line
            val addressLines = orderDetails.address?.split("\n") ?: listOf("N/A")
            val addressWithoutFirstLine = if (addressLines.size > 1) {
                addressLines.drop(1).joinToString("\n") // Drop the first line and join the rest
            } else {
                "N/A" // If there's only one line, show "N/A"
            }
            binding.address.text = addressWithoutFirstLine
            binding.date.text = "Ordered On: ${orderDetails.orderDate}"
            // Extract and display the first line (name) from the address

            val name = addressLines.firstOrNull() ?: "N/A" // Get the first line or "N/A" if empty
            binding.name.text = name
            val statusMessage = statusMessagesMap[orderDetails.itemPushKey]
            if (statusMessage != null) {
                binding.message.text = statusMessage
            } else if (orderDetails.cancellationMessage?.isNotBlank() == true) {
                binding.cancel.visibility = View.VISIBLE
                binding.cancel.text = orderDetails.cancellationMessage
                binding.orderDetails.alpha = 0.4f
                binding.orderDetails.isClickable = false
                binding.orderDetails.isFocusable = false
                binding.orderDetails.setCardBackgroundColor(Color.parseColor("#B6E4E4"))
            } else {
                binding.cancel.visibility = View.GONE
                binding.orderDetails.alpha = 1.0f
                binding.orderDetails.isClickable = true
                binding.orderDetails.isFocusable = true
                binding.orderDetails.setCardBackgroundColor(Color.WHITE)
            }
            // Listen for real-time updates on the message from Firebase
            val statusRef = FirebaseDatabase.getInstance().getReference("OrderDetails").child(orderDetails.itemPushKey ?: "").child("Status")
            statusRef.child("message").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val message = snapshot.getValue(String::class.java)
                    if (message != null) {
                        // Show the updated message in the TextView
                        binding.message.text = message
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.message.text = "Failed to load message"
                }
            })
        }
    }
}