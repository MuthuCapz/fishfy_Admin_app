package com.capztone.admin.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.admin.R
import com.capztone.admin.databinding.CategoryItemBinding
import com.capztone.admin.models.Category
import com.capztone.admin.ui.activities.AddItemActivity
import com.capztone.admin.ui.activities.DiscountActivity
import com.capztone.admin.ui.activities.GeneralAddItemActivity
import com.capztone.admin.ui.activities.GeneralDiscount

import com.capztone.admin.ui.activities.MainActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class GeneralCategoryAdapter1(
    private val categoryList: MutableList<Category>,
    private val listener: MainActivity
) : RecyclerView.Adapter<GeneralCategoryAdapter1.CategoryViewHolder>() {

    private var shopId: String? = null

    // Method to update the shopName in the adapter
    fun updateShopName(shopId: String) {
        this.shopId = shopId
    }

    inner class CategoryViewHolder(private val binding: CategoryItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Category) {
            binding.categoryNameTextView.text = category.name

            // Load image using Glide
            Glide.with(binding.root.context)
                .load(category.image)
                .placeholder(R.drawable.imageplaceholder)
                .into(binding.categoryImageView)

            // Check if the category is "discount" and handle it explicitly
            if (category.name.equals("discount", ignoreCase = true)) {
                shopId?.let { shop ->
                    val discountRef = FirebaseDatabase.getInstance()
                        .getReference("Shops")
                        .child(shop)
                        .child("discount") // Ensure the reference is correct for "discount"

                    discountRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            // Count the number of SKUs inside the "discount" category
                            val skuCount = snapshot.childrenCount
                            // Set the count in the TextView
                            binding.count.text = skuCount.toString()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error if necessary
                        }
                    })
                }
            } else {
                // Normal handling for other categories
                shopId?.let { shop ->
                    val categoryRef = FirebaseDatabase.getInstance()
                        .getReference("Shops")
                        .child(shop)
                        .child(category.name) // Reference to the specific category

                    categoryRef.addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            // Count the number of SKUs inside the category
                            val skuCount = snapshot.childrenCount
                            // Set the count in the TextView
                            binding.count.text = skuCount.toString()
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Handle error if necessary
                        }
                    })
                }
            }

// Set click listener for the entire item view
            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent: Intent

                // Check if the category name is "discount"
                if (category.name.equals("discount", ignoreCase = true)) {
                    // Open DiscountActivity if category name is "discount"
                    intent = Intent(context, GeneralDiscount::class.java)
                } else {
                    // Open AddItemActivity for other categories
                    intent = Intent(context, GeneralAddItemActivity::class.java)
                    // Pass the category name to the AddItemActivity
                    intent.putExtra("categoryName", category.name)
                }

                context.startActivity(intent)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = CategoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(categoryList[position])
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }
}