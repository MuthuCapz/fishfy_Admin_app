package com.capztone.admin.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.admin.databinding.ItemAllItemBinding
import com.capztone.admin.models.AllMenu
import com.capztone.admin.models.DiscountItem
import com.capztone.admin.ui.activities.AllItemsActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class MenuItemAdapter(
    private val context: Context,
    private val menuList: List<Any>,
    databaseReference: DatabaseReference,

    ) : RecyclerView.Adapter<MenuItemAdapter.AllItemViewHolder>() {

    interface OnItemClicked {

        fun updateProducts()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllItemViewHolder {
        val binding = ItemAllItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AllItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AllItemViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = menuList.size

    inner class AllItemViewHolder(private val binding: ItemAllItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {


            // Determine the type of item
            val item = menuList[position]
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
                if (menuItem.adminId == currentUserID) {
                    itemFoodNameTextView.text = menuItem.foodName
                    itemPriceTextView.text = menuItem.foodPrice
                    quantityTextView.text = menuItem.quantity
                    SkuId.text = menuItem.key
                    allmenuCategory.text = menuItem.category
                    Glide.with(context).load(uri).into(itemImageView)

                    deleteImageButton.setOnClickListener {
                        val skuId = menuItem.key
                        if (skuId != null) {
                            (context as? AllItemsActivity)?.deleteItem(skuId)
                        }
                    }
                }
            }
        }

        private fun bindDiscountItem(discountItem: DiscountItem) {
            val uriString = discountItem.foodImages
            val uri = Uri.parse(uriString)

            binding.apply {
                // Check if the item belongs to the current user
                val currentUserID = FirebaseAuth.getInstance().currentUser?.uid
                if (discountItem.adminId == currentUserID) {
                    itemFoodNameTextView.text = discountItem.foodNames
                    itemPriceTextView.text = discountItem.foodPrices
                    quantityTextView.text = discountItem.quantitys
                    SkuId.text = discountItem.key
                    allmenuCategory.text = discountItem.categorys
                    Glide.with(context).load(uri).into(itemImageView)

                    deleteImageButton.setOnClickListener {
                        val skuId = discountItem.key
                        if (skuId != null) {
                            (context as? AllItemsActivity)?.deleteItem(skuId)
                        }
                    }
                }
            }
        }
    }
}