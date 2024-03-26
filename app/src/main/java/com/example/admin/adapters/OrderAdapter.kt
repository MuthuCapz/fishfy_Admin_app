package com.example.admin.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.admin.databinding.ItemTotalOrdersBinding
import com.example.admin.models.OrderDetails


class OrderAdapter(private var orderDetailsList: List<OrderDetails>) : RecyclerView.Adapter<OrderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemTotalOrdersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    private val statusMessagesMap = HashMap<String, String>()

    fun updateStatusMessage(orderId: String, message: String) {
        statusMessagesMap[orderId] = message
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val orderDetails = orderDetailsList[position]
        holder.bind(orderDetails)
    }

    fun setData(newData: List<OrderDetails>) {
        orderDetailsList = newData
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return orderDetailsList.size
    }

    inner class ViewHolder(private val binding: ItemTotalOrdersBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(orderDetails: OrderDetails) {
            binding.itemPushKey.text = "Order id: ${orderDetails.itemPushKey}"
            binding.userUid.text = "User Id: ${orderDetails.userUid}"
            binding.foodNames.text = "Food Names: ${orderDetails.foodNames.joinToString(",")}"
            binding.foodQuantities.text =
                "Quantity: ${orderDetails.foodQuantities.joinToString(",")}"
            binding.foodPrices.text = "Food Price: ${orderDetails.totalPrice}"
            binding.userName.text = "UserName: ${orderDetails.userName}"
            binding.address.text = "Address: ${orderDetails.address}"
            binding.phone.text = "Phone: ${orderDetails.phone}"
            binding.itemPushKey.text = "Order id: ${orderDetails.itemPushKey}"
            if (statusMessagesMap.containsKey(orderDetails.itemPushKey)) {
                binding.message.text = statusMessagesMap[orderDetails.itemPushKey]
            }

            else if (orderDetails.cancellationMessage.isNotBlank()) {
                binding.cancel.visibility = View.VISIBLE
                binding.cancel.text = "${orderDetails.cancellationMessage}"
                binding.orderDetails.alpha = 0.4f
                binding.orderDetails.isClickable = false
                binding.orderDetails.isFocusable = false
                binding.orderDetails.setCardBackgroundColor(Color.parseColor("#B6E4E4"))
            }

            else {
                binding.cancel.visibility = View.GONE

                // Enable the CardView
                binding.orderDetails.alpha = 1.0f
                binding.orderDetails.isClickable = true
                binding.orderDetails.isFocusable = true
                binding.orderDetails.setCardBackgroundColor(Color.WHITE)
            }
        }
    }
}
