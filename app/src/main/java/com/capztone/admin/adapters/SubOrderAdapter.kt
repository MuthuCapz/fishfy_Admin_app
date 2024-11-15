package com.capztone.admin.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.capztone.admin.databinding.SubOrderDetailsBinding
import com.capztone.admin.models.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class SubOrderAdapter(
    private var orderDetailsList: MutableList<OrderDetails>,
    private val noOrdersTextView: TextView,
    private val orderStatus: String
) : RecyclerView.Adapter<SubOrderAdapter.ViewHolder>() {

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
        val binding = SubOrderDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        } else {
            noOrdersTextView.visibility = View.GONE
        }
        return orderDetailsList.size
    }

    fun fetchOrderDetailsFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (shopName == null || userId == null) {
            Log.e("SubOrderAdapter", "Shop name or User ID is null. Cannot fetch order details.")
            return
        }

        Log.d("SubOrderAdapter", "Order status from TextView passed to adapter: $orderStatus")

        // Reference the correct path for order details under OrderDetails
        val orderDetailsRef = FirebaseDatabase.getInstance().getReference("OrderDetails")

        // Fetch all order details from OrderDetails path
        orderDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                orderDetailsList.clear()
                deliveredOrderCount = 0

                for (snapshot in dataSnapshot.children) {
                    val orderId = snapshot.key ?: ""
                    val orderDetails = snapshot.getValue(OrderDetails::class.java)
                    Log.d("SubOrderAdapter", "OrderDetails: $orderDetails")

                    if (orderDetails != null && shopName in orderDetails.shopNames) {
                        // Fetch the specific status message for each order
                        val statusRef = orderDetailsRef.child(orderId).child("Status").child("message")

                        // Listen for the message under Status -> message for the specific order ID
                        statusRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(messageSnapshot: DataSnapshot) {
                                val message = messageSnapshot.getValue(String::class.java)
                                Log.d("SubOrderAdapter", "Status message for order $orderId: $message")

                                when (orderStatus) { // Use the passed orderStatus
                                    "New Orders" -> {
                                        // Check if there's a cancellation message. If there is, skip this order.
                                        val cancellationMessageRef = orderDetailsRef.child(orderId).child("cancellationMessage")
                                        cancellationMessageRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(cancellationMessageSnapshot: DataSnapshot) {
                                                val cancellationMessage = cancellationMessageSnapshot.getValue(String::class.java)
                                                // Add the order only if cancellationMessage is blank
                                                if (cancellationMessage.isNullOrBlank() && message.isNullOrBlank()) {
                                                    orderDetailsList.add(orderDetails)
                                                    notifyDataSetChanged()
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                               // Log.e("SubOrderAdapter", "Error fetching cancellation message: ${error.message}")
                                            }
                                        })
                                    }
                                    "Ongoing Orders" -> {
                                        // Add orders that have "Order Confirmed" status
                                        if (message == "Order confirmed" || message == "Order picked") {
                                            orderDetailsList.add(orderDetails)
                                        }
                                    }
                                    "Completed Orders" -> {
                                        // Add orders that have "Order Delivered" status
                                        if (message == "Order delivered") {
                                            orderDetailsList.add(orderDetails)
                                            deliveredOrderCount++
                                        }
                                    }
                                    "Cancel Orders" -> {
                                        // Add orders that have a cancellation message
                                        val cancellationMessageRef = orderDetailsRef.child(orderId).child("cancellationMessage")

                                        cancellationMessageRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                            override fun onDataChange(cancellationMessageSnapshot: DataSnapshot) {
                                                val cancellationMessage = cancellationMessageSnapshot.getValue(String::class.java)
                                                if (!cancellationMessage.isNullOrBlank()) {
                                                    orderDetailsList.add(orderDetails)
                                                    Log.d("SubOrderAdapter", "Order $orderId retrieved with cancellation message: $cancellationMessage")
                                                }
                                                notifyDataSetChanged()
                                            }

                                            override fun onCancelled(error: DatabaseError) {
                                                //Log.e("SubOrderAdapter", "Error fetching cancellation message: ${error.message}")
                                            }
                                        })
                                    }
                                }
                                // Notify after status message has been processed
                                notifyDataSetChanged()
                            }

                            override fun onCancelled(error: DatabaseError) {
                                //Log.e("SubOrderAdapter", "Error fetching status message: ${error.message}")
                            }
                        })
                    }
                }

                // Notify that the list has changed after processing all orders
                notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                //Log.e("SubOrderAdapter", "Error fetching order details: ${databaseError.message}")
            }
        })
    }



    fun getDeliveredOrderCount(): Int {
        return deliveredOrderCount
    }

    inner class ViewHolder(private val binding: SubOrderDetailsBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            // Set up the click listener for the confirm order button
            binding.confirmOrder.setOnClickListener {
                // Get the orderId from the current order details
                val orderId = binding.itemPushKey.text.toString().trim()
                val shopName = binding.shopnames.text.toString().trim()

                // Check if the orderId matches with any order in Firebase
                checkAndConfirmOrder(orderId)
                reduce(orderId, shopName)
            }
        }

        // Example adjustment to reduce transaction usage and better error handling
        private fun reduce(orderId: String, shopName: String) {
            val orderDetails = orderDetailsList.find { it.itemPushKey == orderId }
            if (orderDetails == null) {
                showToast("Order not found for ID: $orderId")
                return
            }

            // Get the SKU list and quantities
            val skuIds = orderDetails.skuList ?: return
            val quantities = orderDetails.foodQuantities ?: return

            // Ensure we have at least one SKU ID and quantity to work with
            if (skuIds.isEmpty() || quantities.isEmpty() || skuIds.size != quantities.size) {
                showToast("No SKU IDs or quantities available")
                return
            }

            // Log the SKU IDs and their corresponding quantities for debugging
            for (i in skuIds.indices) {
                val skuId = skuIds[i]
                val quantityToReduce = quantities[i].toString().toDoubleOrNull() ?: continue  // Skip if quantity can't be parsed

                // Log the SKU ID and its quantity for debugging
                Log.d("ReduceFunction", "Processing SKU ID: $skuId with quantity to reduce: $quantityToReduce")

                // Reference to the shop
                val shopRef = FirebaseDatabase.getInstance().getReference("Shops").child(shopName)

                // Fetch the SKU from the shop
                shopRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        var skuFound = false

                        // Iterate through all categories and their SKUs
                        for (categorySnapshot in dataSnapshot.children) {
                            for (skuSnapshot in categorySnapshot.children) {
                                if (skuSnapshot.key == skuId) {
                                    skuFound = true
                                    updateQuantity(skuSnapshot.ref, quantityToReduce, skuId)
                                    break // Exit the inner loop once the SKU is found and processed
                                }
                            }
                            if (skuFound) break // Exit the outer loop if SKU is found
                        }

                        if (!skuFound) {
                            // SKU ID not found in the shop
                            //Log.e("ReduceFunction", "SKU ID $skuId not found in shop: $shopName")
                            showToast("SKU ID $skuId not found in shop: $shopName")
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        showToast("Error checking SKU ID: ${databaseError.message}")
                    }
                })
            }
        }

        private fun updateQuantity(skuRef: DatabaseReference, quantityToReduce: Double, skuId: String) {
            skuRef.get().addOnSuccessListener { snapshot ->
                val currentQuantityStr = snapshot.child("quantity").getValue(String::class.java)
                    ?: snapshot.child("quantitys").getValue(String::class.java)
                val currentQuantityValue = currentQuantityStr?.removeSuffix("kg")?.toDoubleOrNull()

                Log.d("UpdateQuantity", "Current quantity string: $currentQuantityStr, Current quantity value: $currentQuantityValue")

                if (currentQuantityValue == null) {
                    Log.e("UpdateQuantity", "Current quantity is null or invalid for SKU: $skuId")
                    showToast("Current quantity is null or invalid for SKU: $skuId")
                    return@addOnSuccessListener
                }

                val newQuantityValue = currentQuantityValue - quantityToReduce
                Log.d("QuantityCheck", "Current Quantity Value: $currentQuantityValue")
                Log.d("QuantityCheck", "Quantity to Reduce: $quantityToReduce")
                Log.d("QuantityCheck", "New Quantity Value: $newQuantityValue")

                if (newQuantityValue < 0) {
                    Log.e("UpdateQuantity", "New quantity would be negative for SKU: $skuId")
                    showToast("New quantity would be negative for SKU: $skuId")
                    return@addOnSuccessListener
                }
                if (newQuantityValue == 0.0) {
                    // Don't delete the item, just update the quantity to 0kg
                    Log.d("UpdateQuantity", "Quantity reached zero for SKU: $skuId. No deletion performed.")

                    // Set the quantity to 0kg instead of deleting the item
                    skuRef.child("quantity").setValue("0kg").addOnCompleteListener { updateQuantityTask ->
                        if (updateQuantityTask.isSuccessful) {
                            showToast("Quantity for SKU $skuId updated to 0kg without deletion.")
                            Log.d("UpdateQuantity", "Quantity for SKU $skuId updated to 0kg.")
                        } else {
                            Log.e("UpdateQuantity", "Failed to update quantity to 0kg for SKU: $skuId: ${updateQuantityTask.exception?.message}")
                            showToast("Failed to update quantity to 0kg for SKU: $skuId")
                        }
                    }

                } else {
                    // Round the new quantity value to the nearest whole number
                    val roundedQuantityValue = newQuantityValue.roundToInt()

                    // Determine which child to update based on which one exists
                    val quantityField = if (snapshot.hasChild("quantity")) "quantity" else "quantitys"

                    skuRef.child(quantityField).setValue("${roundedQuantityValue}kg").addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // showToast("Quantity reduced successfully for SKU: $skuId!")
                        } else {
                            Log.e(
                                "UpdateQuantity",
                                "Failed to reduce quantity for SKU: $skuId: ${task.exception?.message}"
                            )
                            showToast("Failed to reduce quantity for SKU: $skuId: ${task.exception?.message}")
                        }
                    }
                }

            }.addOnFailureListener { e ->
                Log.e("UpdateQuantity", "Failed to get current quantity: ${e.message}")
                showToast("Failed to get current quantity: ${e.message}")
            }
        }

        private fun showToast(message: String) {
            Toast.makeText(binding.root.context, message, Toast.LENGTH_SHORT).show()
        }

        private fun checkAndConfirmOrder(orderId: String) {
            databaseReference.child(orderId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Order found, update the confirmation message
                        val orderRef = databaseReference.child(orderId)
                        orderRef.child("Confirmation").setValue("Order Confirmed").addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(binding.root.context, "Order Confirmed!", Toast.LENGTH_SHORT).show()
                                binding.confirmOrder.text = "Order Comfirmed"
                                binding.confirmOrder.isEnabled = false

                            } else {
                                Toast.makeText(binding.root.context, "Failed to confirm order", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Order not found
                        Toast.makeText(binding.root.context, "Order ID not found", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                    Toast.makeText(binding.root.context, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
        fun bind(orderDetails: OrderDetails) {
            binding.itemPushKey.text = "${orderDetails.itemPushKey ?: "N/A"}"
            binding.foodNames.text = "${orderDetails.foodNames?.joinToString(",") ?: "N/A"}"
            binding.foodQuantities.text = "${orderDetails.foodQuantities?.joinToString(",") ?: "N/A"}"
            binding.foodPrices.text = "${orderDetails.foodPrices?.joinToString(",") ?: "N/A"}"
            binding.skuid.text = "${orderDetails.skuList?.joinToString(",") ?: "N/A"}"
            binding.shopnames.text = orderDetails.shopNames?.getOrNull(0) ?: "N/A"
            binding.slot.text = "${orderDetails.selectedSlot}"
            // Extract and display the address without the first line
            val addressLines = orderDetails.address?.split("\n") ?: listOf("N/A")
            val addressWithoutFirstLine = if (addressLines.size > 1) {
                addressLines.drop(1).joinToString("\n") // Drop the first line and join the rest
            } else {
                "N/A" // If there's only one line, show "N/A"
            }
            binding.address.text = addressWithoutFirstLine
            binding.date.text = "Order On: ${orderDetails.orderDate}"
            // Extract and display the first line (name) from the address

            val name = addressLines.firstOrNull() ?: "N/A" // Get the first line or "N/A" if empty
            binding.name.text = name

            // Fetch the confirmation status from Firebase
            val orderRef = FirebaseDatabase.getInstance().getReference("OrderDetails").child(orderDetails.itemPushKey ?: "")
            orderRef.child("Confirmation").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val confirmationStatus = snapshot.getValue(String::class.java) ?: ""
                    if (confirmationStatus == "Order Confirmed") {
                        binding.confirmOrder.text = "Order Comfirmed"
                        binding.confirmOrder.isEnabled = false
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error if needed
                }
            })

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
                        binding.message.text = "Status: $message"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.message.text = "Failed to load message"
                }
            })
        }
    }



}