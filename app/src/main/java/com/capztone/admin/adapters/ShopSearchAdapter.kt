package com.capztone.admin.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capztone.admin.databinding.ShopSearchItemBinding
import com.capztone.admin.models.SearchShop


class ShopSearchAdapter(
    private val shopList: List<SearchShop>,
    private val onShopClick: (String) -> Unit
) : RecyclerView.Adapter<ShopSearchAdapter.ShopViewHolder>() {

    inner class ShopViewHolder(val binding: ShopSearchItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(shop: SearchShop) {
            binding.shopId.text = shop.shopId
            binding.shopName.text = shop.shopName

            // Customize recent searches appearance (optional)
            if (shop.shopName == shop.shopId) {
                // This is a recent search item, apply a different style if needed
                binding.shopName.setTextColor(Color.GRAY) // Example style change
            } else {
                // Regular search item style
                binding.shopName.setTextColor(Color.BLACK)
            }

            // Set the click listener
            binding.root.setOnClickListener {
                shop.shopId?.let { it1 -> onShopClick(it1) } // Pass the shopId to the click listener
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ShopSearchItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ShopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = shopList[position]
        holder.bind(shop) // Use the bind method
    }

    override fun getItemCount(): Int = shopList.size
}
