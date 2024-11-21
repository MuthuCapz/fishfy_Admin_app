package com.capztone.admin.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.admin.R
import com.capztone.admin.databinding.AddInventoryItemBinding
import com.capztone.admin.models.DiscountItem
import com.capztone.admin.models.RetrieveItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class InventoryAdapter(private val context: Context,  private val onDataFetched: (Boolean) -> Unit) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    private val database = FirebaseDatabase.getInstance()
    private val currentUserID: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var productsList: MutableList<RetrieveItem> = mutableListOf()
    private var productsList1: MutableList<DiscountItem> = mutableListOf()

    // ViewHolder class
    inner class ViewHolder(private val binding: AddInventoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(retrieveItem: RetrieveItem?, discountItem: DiscountItem?) {
            retrieveItem?.let { product ->
                binding.itemFoodNameTextView.text = product.foodName?.getOrNull(0)
                binding.SkuId.text = product.key
                binding.itemPriceTextView.text = "₹${product.foodPrice.toString()}"
                binding.allmenuCategory.text = product.category.toString()
                binding.quantityTextView.text = "Total Qty: ${product.quantity}"
                binding.quantityunit.text="/ ${product.productQuantity}"

                Glide.with(context)
                    .load(product.foodImage)
                    .into(binding.itemImageView)

                binding.stock.text = product.stock // Update stock status here
            }

            discountItem?.let { discount ->
                binding.itemFoodNameTextView.text = discount.foodNames?.getOrNull(0)
                binding.SkuId.text = discount.key
                binding.itemPriceTextView.text = "₹${discount.foodPrices.toString()}"
                binding.allmenuCategory.text = discount.categorys.toString()
                binding.quantityTextView.text = "Total Qty: ${discount.quantitys}"
                binding.quantityunit.text="/ ${discount.productQuantity}"

                Glide.with(context)
                    .load(discount.foodImages)
                    .into(binding.itemImageView)

                binding.stock.text = discount.stocks
                // Update other views as needed
            }
        }


    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AddInventoryItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < productsList.size) {
            val retrieveItem = productsList[position]
            holder.bind(retrieveItem, null)
        } else {
            val discountItemPosition = position - productsList.size
            val discountItem = productsList1.getOrNull(discountItemPosition)
            holder.bind(null, discountItem)
        }
    }

    override fun getItemCount(): Int {
        return productsList.size + productsList1.size
    }
    @SuppressLint("NotifyDataSetChanged")
    fun fetchData() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

        currentUserID?.let { userId ->
            val adminRef = database.getReference("Delivery Details")
            adminRef.child("Shop Id").get().addOnSuccessListener { snapshot ->
                val shopName = snapshot.getValue(String::class.java)
                if (shopName != null) {
                    // Fetch RetrieveItem items from the shop-specific path
                    val productsRef = database.getReference("Shops").child(shopName).child("Products")
                    productsRef.addValueEventListener(object : ValueEventListener {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            Log.d("InventoryAdapter", "onDataChange called")

                            // Clear existing list to avoid duplicate accumulation on each fetch
                            productsList.clear()
                            val uniqueProducts = mutableSetOf<String>() // For storing unique product IDs or names

                            for (snapshot in dataSnapshot.children) {
                                val product = snapshot.getValue(RetrieveItem::class.java)
                                product?.let {
                                    // Only add if it's not null and not already in the uniqueProducts set
                                    if (it.foodName != null && uniqueProducts.add(it.foodName.toString())) {
                                        productsList.add(it)
                                        Log.d("InventoryAdapter", "Added product: ${it.foodName}")
                                    }
                                }
                            }
                            checkEmptyState()
                            notifyDataSetChanged()
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                        }
                    })

                    // Fetch DiscountItem items from the shop-specific path
                    val discountItemsRef = database.getReference("Shops").child(shopName).child("Products")
                    discountItemsRef.addValueEventListener(object : ValueEventListener {
                        @SuppressLint("NotifyDataSetChanged")
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            // Clear existing list to avoid duplicate accumulation on each fetch
                            productsList1.clear()
                            val uniqueDiscountItems = mutableSetOf<String>() // For storing unique discount item IDs or names

                            for (snapshot in dataSnapshot.children) {
                                val discountItem = snapshot.getValue(DiscountItem::class.java)
                                discountItem?.let {
                                    // Only add if it's not null and not already in the uniqueDiscountItems set
                                    if (it.foodNames != null && uniqueDiscountItems.add(it.foodNames.toString())) {
                                        productsList1.add(it)
                                        Log.d("InventoryAdapter", "Added discount item: ${it.foodNames}")
                                    }
                                }
                            }
                            checkEmptyState()
                            notifyDataSetChanged()
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                        }
                    })
                } else {
                    Log.e("InventoryAdapter", "Shop name not found for user: $userId")
                }
            }.addOnFailureListener { exception ->
                Log.e("InventoryAdapter", "Failed to retrieve shop name: $exception")
            }
        }
    }
    private fun checkEmptyState() {
        // Notify the activity if both lists are empty
        onDataFetched(productsList.isNotEmpty() || productsList1.isNotEmpty())
        notifyDataSetChanged()
    }
    fun getItems(): List<RetrieveItem> {
        return productsList
    }

    fun getDiscountItems(): List<DiscountItem> {
        return productsList1
    }
}