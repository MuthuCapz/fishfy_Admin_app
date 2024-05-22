package com.capztone.admin.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capztone.admin.databinding.ItemTotalOrdersBinding
import com.capztone.admin.models.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrderAdapter(private var orderDetailsList: MutableList<OrderDetails>) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

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
            val adminRef = FirebaseDatabase.getInstance().getReference("Admins").child(userId)
            adminRef.child("shopName").addListenerForSingleValueEvent(object : ValueEventListener {
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
            binding.itemPushKey.text = "Order id: ${orderDetails.itemPushKey ?: "N/A"}"
            binding.userUid.text = "User Id: ${orderDetails.userUid ?: "N/A"}"
            binding.foodNames.text = "Food Names: ${orderDetails.foodNames?.joinToString(",") ?: "N/A"}"
            binding.foodQuantities.text = "Quantity: ${orderDetails.foodQuantities?.joinToString(",") ?: "N/A"}"
            binding.foodPrices.text = "Food Price: ${orderDetails.foodPrices?.joinToString(",") ?: "N/A"}"
            binding.userName.text = "UserName: ${orderDetails.userName ?: "N/A"}"
            binding.address.text = "Address: ${orderDetails.address ?: "N/A"}"
            binding.phone.text = "Phone: ${orderDetails.phone ?: "N/A"}"

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
        }
    }
}
