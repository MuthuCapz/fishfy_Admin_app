package com.capztone.admin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capztone.admin.databinding.ItemOrderDetailsBinding
import com.capztone.admin.models.Order

class OrderDetailsAdapter : RecyclerView.Adapter<OrderDetailsAdapter.OrderViewHolder>() {

    private var orders = emptyList<Order>()

    fun setOrders(orders: List<Order>) {
        this.orders = orders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemOrderDetailsBinding.inflate(inflater, parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size
    fun getOrders(): List<Order> = orders

    inner class OrderViewHolder(private val binding: ItemOrderDetailsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(order: Order) {
            val foodNameText = "Food Name : ${order.foodNames}"
            val foodQuantityText = "Quantity : ${order.foodQuantities}"


            binding.foodName.text = foodNameText
            binding.foodQuantity.text = foodQuantityText

        }
    }
}
