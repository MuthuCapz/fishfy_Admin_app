package com.capztone.admin.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.admin.databinding.ItemAllItemBinding
import com.capztone.admin.models.AllMenu
import com.capztone.admin.models.DiscountItem
import com.capztone.admin.ui.activities.AllItemsActivity
import com.capztone.admin.ui.activities.DiscountProductEditActivity
import com.capztone.admin.ui.activities.ProductEditActivity

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MenuItemAdapter(
    private val context: Context,
    private var items: List<Any>,
    private val databaseReference: DatabaseReference,
    private val noProductTextView: TextView,
    private val button: Button,
) : RecyclerView.Adapter<MenuItemAdapter.AllItemViewHolder>() {

    init {
        // Update visibility when the adapter is initialized or when the data changes
        updateNoProductVisibility()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllItemViewHolder {
        val binding = ItemAllItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AllItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AllItemViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = items.size

    // Method to update data and notify the adapter
    // Method to update data and notify the adapter, ensuring no duplicate products
    fun updateItems(newItems: List<Any>) {
        val uniqueItems = newItems.distinctBy {
            when (it) {
                is AllMenu -> it.key // Use key for AllMenu items
                is DiscountItem -> it.key // Use key for DiscountItem items
                else -> null // Handle other item types if necessary
            }
        }
        items = uniqueItems
        // Update visibility after data change
        updateNoProductVisibility()
        notifyDataSetChanged()


    }

    // Method to update visibility of NoProduct TextView
    private fun updateNoProductVisibility() {
        // Initially, set the TextView to invisible
        noProductTextView.visibility = View.INVISIBLE
        button.visibility = View.INVISIBLE

        // Delay for 3 seconds before making the TextView visible if the list is empty
        Handler(Looper.getMainLooper()).postDelayed({
            if (items.isEmpty()) {
                noProductTextView.visibility = View.VISIBLE // Show "NoProduct" message
                button.visibility = View.GONE

            } else {
                noProductTextView.visibility = View.GONE // Hide the message if there are items
                button.visibility = View.VISIBLE
            }
        }, 1600) // 3000 milliseconds = 3 seconds
    }

    inner class AllItemViewHolder(private val binding: ItemAllItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            // Determine the type of item
            val item = items[position]
            if (item is AllMenu) {
                bindAllMenuItem(item)
            } else if (item is DiscountItem) {
                bindDiscountItem(item)
            }
        }

        private fun bindAllMenuItem(menuItem: AllMenu) {
            val uriString = menuItem.foodImage
            val uri = Uri.parse(uriString)

            binding.apply {
                // Check if the item belongs to the current user
                val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

                // Format the foodName list to remove brackets and join the names with new lines
                val formattedFoodName = menuItem.foodName?.joinToString(separator = "\n") ?: ""

                // Bind data to views
                itemFoodNameTextView.text = formattedFoodName
                itemPriceTextView.text = "₹ ${menuItem.foodPrice}"
                quantityTextView.text = "Total Qty: ${menuItem.quantity}"
                SkuId.text = menuItem.key
                stock.text = menuItem.stock
                allmenuCategory.text = menuItem.category
                quantityUnit.text = "/ ${menuItem.productQuantity}" // Display the product quantity with a preceding slash
                Glide.with(context).load(uri).into(itemImageView)

                deleteImageButton.setOnClickListener {
                    val skuId = menuItem.key
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Get the current user's ID
                    val allUsersRef = FirebaseDatabase.getInstance().getReference("user")
                    val orderDetailsRef = FirebaseDatabase.getInstance().getReference("OrderDetails")

                    if (skuId != null) {
                        // Fetch all OrderDetails to check if the skuId exists in a confirmed order's skuList
                        orderDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(orderSnapshot: DataSnapshot) {
                                var skuExistsInConfirmedOrder = false

                                // Iterate over each order in OrderDetails
                                for (orderSnapshot in orderSnapshot.children) {
                                    val confirmationStatus = orderSnapshot.child("Status").child("message").value as? String
                                    val skuListSnapshot = orderSnapshot.child("skuList")

                                    // Check if the order has Confirmation: "Order Confirmed"
                                    if (confirmationStatus == "Order confirmed") {
                                        // Check each SKU in the skuList for this order
                                        for (skuSnapshot in skuListSnapshot.children) {
                                            val orderSkuId = skuSnapshot.value as? String
                                            if (orderSkuId == skuId) {
                                                skuExistsInConfirmedOrder = true
                                                break // Exit loop if SKU found
                                            }
                                        }
                                    }

                                    if (skuExistsInConfirmedOrder) break // Exit outer loop if SKU found
                                }

                                // Now check if SKU exists in the confirmed orders
                                if (skuExistsInConfirmedOrder) {
                                    // Show the SKU existing dialog if SKU ID is found in any order's confirmed skuList
                                    (context as? AllItemsActivity)?.skuExistingCartDialog()
                                } else {
                                    // Proceed to check users' cartItems for the SKU ID
                                    allUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            var skuExistsInCart = false

                                            // Iterate over each user's cartItems
                                            for (userSnapshot in snapshot.children) {
                                                val cartItemsSnapshot = userSnapshot.child("cartItems")

                                                // Check each SKU in the cartItems path for this user
                                                for (cartItemSnapshot in cartItemsSnapshot.children) {
                                                    val cartSkuId = cartItemSnapshot.key
                                                    if (cartSkuId == skuId) {
                                                        skuExistsInCart = true
                                                        break // Exit loop if SKU found
                                                    }
                                                }

                                                if (skuExistsInCart) break // Exit outer loop if SKU found
                                            }

                                            if (skuExistsInCart) {
                                                // Show the delete confirmation dialog if SKU exists in any user's cartItems
                                                (context as? AllItemsActivity)?.skuExistingCartDialog()
                                            } else {
                                                // Show a different dialog if the SKU ID does not match any in cartItems
                                                (context as? AllItemsActivity)?.showDeleteConfirmationDialog(skuId)
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Handle potential errors here
                                        }
                                    })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle potential errors here
                            }
                        })
                    }
                }


                // Handle edit button click to navigate to ProductEditActivity
                edit.setOnClickListener {
                    val intent = Intent(context, ProductEditActivity::class.java)
                    intent.putExtra("category", menuItem.category)
                    intent.putExtra("skuId", menuItem.key)
                    intent.putExtra("names", formattedFoodName)
                    intent.putExtra("prices", menuItem.foodPrice)
                    intent.putExtra("image", uriString)
                    intent.putExtra("quantity", menuItem.quantity)
                    intent.putExtra("productquantity", menuItem.productQuantity)
                    intent.putExtra("description", menuItem.foodDescription)
                    context.startActivity(intent)
                }

            }
        }

        private fun bindDiscountItem(discountItem: DiscountItem) {
            val uriString = discountItem.foodImages
            val uri = Uri.parse(uriString)

            binding.apply {
                // Check if the item belongs to the current user
                val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

                // Format the foodNames list to remove brackets and join the names with new lines
                val formattedFoodNames =
                    discountItem.foodNames?.joinToString(separator = "\n") ?: ""

                // Bind data to views
                itemFoodNameTextView.text = formattedFoodNames
                itemPriceTextView.text = "₹ ${discountItem.foodPrices}"
                quantityTextView.text = "Total Qty: ${discountItem.quantitys}"
                SkuId.text = discountItem.key
                stock.text = discountItem.stocks
                allmenuCategory.text = discountItem.categorys
                quantityUnit.text = "/ ${discountItem.productQuantity}" // Display the product quantity with a preceding slash
                Glide.with(context).load(uri).into(itemImageView)

                // Handle delete button click
                deleteImageButton.setOnClickListener {
                    val skuId = discountItem.key
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Get the current user's ID
                    val allUsersRef = FirebaseDatabase.getInstance().getReference("user")
                    val orderDetailsRef = FirebaseDatabase.getInstance().getReference("OrderDetails")

                    if (skuId != null) {
                        // Fetch all OrderDetails to check if the skuId exists in a confirmed order's skuList
                        orderDetailsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(orderSnapshot: DataSnapshot) {
                                var skuExistsInConfirmedOrder = false

                                // Iterate over each order in OrderDetails
                                for (orderSnapshot in orderSnapshot.children) {
                                    val confirmationStatus = orderSnapshot.child("Status").child("message").value as? String
                                    val skuListSnapshot = orderSnapshot.child("skuList")

                                    // Check if the order has Confirmation: "Order Confirmed"
                                    if (confirmationStatus == "Order confirmed") {
                                        // Check each SKU in the skuList for this order
                                        for (skuSnapshot in skuListSnapshot.children) {
                                            val orderSkuId = skuSnapshot.value as? String
                                            if (orderSkuId == skuId) {
                                                skuExistsInConfirmedOrder = true
                                                break // Exit loop if SKU found
                                            }
                                        }
                                    }

                                    if (skuExistsInConfirmedOrder) break // Exit outer loop if SKU found
                                }

                                // Now check if SKU exists in the confirmed orders
                                if (skuExistsInConfirmedOrder) {
                                    // Show the SKU existing dialog if SKU ID is found in any order's confirmed skuList
                                    (context as? AllItemsActivity)?.skuExistingCartDialog()
                                } else {
                                    // Proceed to check users' cartItems for the SKU ID
                                    allUsersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            var skuExistsInCart = false

                                            // Iterate over each user's cartItems
                                            for (userSnapshot in snapshot.children) {
                                                val cartItemsSnapshot = userSnapshot.child("cartItems")

                                                // Check each SKU in the cartItems path for this user
                                                for (cartItemSnapshot in cartItemsSnapshot.children) {
                                                    val cartSkuId = cartItemSnapshot.key
                                                    if (cartSkuId == skuId) {
                                                        skuExistsInCart = true
                                                        break // Exit loop if SKU found
                                                    }
                                                }

                                                if (skuExistsInCart) break // Exit outer loop if SKU found
                                            }

                                            if (skuExistsInCart) {
                                                // Show the delete confirmation dialog if SKU exists in any user's cartItems
                                                (context as? AllItemsActivity)?.skuExistingCartDialog()
                                            } else {
                                                // Show a different dialog if the SKU ID does not match any in cartItems
                                                (context as? AllItemsActivity)?.showDeleteConfirmationDialog(skuId)
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            // Handle potential errors here
                                        }
                                    })
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // Handle potential errors here
                            }
                        })
                    }
                }

                // Handle edit button click to navigate to ProductEditActivity
                edit.setOnClickListener {
                    val intent = Intent(context, DiscountProductEditActivity::class.java)
                    intent.putExtra("category", discountItem.categorys)
                    intent.putExtra("skuId", discountItem.key)
                    intent.putExtra("names", formattedFoodNames)
                    intent.putExtra("prices", discountItem.foodPrices)
                    intent.putExtra("image", uriString)
                    intent.putExtra("quantity", discountItem.quantitys)
                    intent.putExtra("productquantity", discountItem.productQuantity)
                    intent.putExtra("discounts", discountItem.discounts)
                    intent.putExtra("description", discountItem.foodDescriptions)
                    context.startActivity(intent)
                }
            }
        }
    }
}