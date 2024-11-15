package com.capztone.admin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import android.content.Context

import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.capztone.admin.databinding.ItemViewShopBinding
import com.capztone.admin.models.ViewShop
import com.capztone.admin.ui.activities.AllItemsActivity
import com.capztone.admin.ui.activities.ViewShopActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ViewShopAdapter(
    private val context: Context,
    private val shopList: MutableList<ViewShop>,
    private val onShopClick: (String) -> Unit, // Callback for item clicks
    private val onDeleteClick: (String) -> Unit // Add this parameter for delete action
) : RecyclerView.Adapter<ViewShopAdapter.ShopViewHolder>() {

    inner class ShopViewHolder(val binding: ItemViewShopBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(shop: ViewShop) {
            binding.shopId.text = shop.shopId
            binding.shopName.text = shop.shopName
            binding.mobileNumber.text = "+91 ${shop.mobileNumber}"
            // Set the click listener

            binding.btnEdit.setOnClickListener {
                shop.shopId?.let { onShopClick(it) } // Call edit method
            }
            binding.btnDelete.setOnClickListener {
                shop.shopId?.let { shopId ->
                    checkUsageInCartAndOrderDetails(shopId) { canDelete ->
                        if (canDelete) {
                            onDeleteClick(shopId)
                        } else {
                            (context as? ViewShopActivity)?.skuExistingCartDialog()                }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val binding = ItemViewShopBinding.inflate(
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





// Method to check usage in both "cartItems" and "OrderDetails"
private fun checkUsageInCartAndOrderDetails(shopId: String, callback: (Boolean) -> Unit) {
    val database = FirebaseDatabase.getInstance().reference

    // Check "cartItems" under "users" path
    val usersRef = database.child("user")
    usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            for (userSnapshot in snapshot.children) {
                val cartItemsSnapshot = userSnapshot.child("cartItems")
                for (cartItem in cartItemsSnapshot.children) {
                    val path = cartItem.child("path").value as? String
                    if (path == shopId) {
                        // Match found in "cartItems"
                        callback(false)
                        return
                    }
                }
            }
            // No match found in "cartItems", proceed to check "OrderDetails"
            checkOrderDetailsUsage(shopId, callback)
        }

        override fun onCancelled(error: DatabaseError) {
            callback(false)
        }
    })
}

// Method to check usage in "OrderDetails"
private fun checkOrderDetailsUsage(shopId: String, callback: (Boolean) -> Unit) {
    val orderDetailsRef = FirebaseDatabase.getInstance().reference.child("OrderDetails")

    orderDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            for (orderSnapshot in snapshot.children) {
                // Check if "Status" path exists
                if (!orderSnapshot.child("Status").exists()) {
                    val shopNames = orderSnapshot.child("shopNames").value as? List<String>
                    if (shopNames != null && shopNames.isNotEmpty() && shopNames[0] == shopId) {
                        // "Status" exists and shopId matches in shopNames
                        callback(false)
                        return
                    }
                }
            }
            // No match found with "Status" and shopId, safe to delete
            callback(true)
        }

        override fun onCancelled(error: DatabaseError) {
            callback(false)
        }
    })
}