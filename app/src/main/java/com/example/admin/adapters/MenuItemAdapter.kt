package com.example.admin.adapters

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.admin.databinding.ItemAllItemBinding
import com.example.admin.models.AllMenu
import com.example.admin.ui.activities.AllItemsActivity
import com.google.firebase.database.DatabaseReference

class MenuItemAdapter(
    private val context: Context,
    private val menuList: ArrayList<AllMenu>,
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
            binding.apply {

                val menuItem = menuList[position]
                val uriString = menuItem.foodImage
                val uri = Uri.parse(uriString)

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
}
