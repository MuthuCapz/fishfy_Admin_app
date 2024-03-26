package com.example.admin.adapters



import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.admin.R
import com.example.admin.databinding.AddInventoryItemBinding
import com.example.admin.models.RetrieveItem
import com.google.firebase.database.*

class InventoryAdapter(private val context: Context) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    private val database = FirebaseDatabase.getInstance()
    private val productsRef = database.getReference("products")

    // Data list for the products
    private var productsList: MutableList<RetrieveItem> = mutableListOf()

    // ViewHolder class
    inner class ViewHolder(private val binding: AddInventoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set click listener for the ProductImage ImageView
            binding.dropdown.setOnClickListener { view ->
                // Show popup menu when the image is clicked
                showPopupMenu(view, adapterPosition)
            }
        }

        fun bind(product: RetrieveItem) {
            binding.itemName.text = product.foodName
            binding.SkuID.text = product.key
            binding.itemPrice.text = product.foodPrice.toString()
            binding.itemCategory.text = product.category.toString()
            binding.quantity.text = product.quantity

            Glide.with(context)
                .load(product.foodImage) // Use the URL from your Menu data class
                .into(binding.ProductImage)

            binding.stock.text = product.stock // Update stock status here
        }

        private fun showPopupMenu(view: View, position: Int) {
            val popupMenu = PopupMenu(context, view)
            popupMenu.inflate(R.menu.stock)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_in_stock -> {
                        // Update stock status to "In Stock"
                        updateStockStatus(position, "In Stock")
                        true
                    }
                    R.id.menu_out_stock -> {
                        // Update stock status to "Out of Stock"
                        updateStockStatus(position, "Out of Stock")
                        true
                    }
                    // Add more menu options if needed
                    else -> false
                }
            }

            popupMenu.show()
        }

        private fun updateStockStatus(position: Int, stockStatus: String) {
            val product = productsList[position]

            // Update the local list
            product.stock = stockStatus
            notifyItemChanged(position)

            // Update Firebase
            val ref = productsRef.child(product.key!!)

            ref.child("stock").setValue(stockStatus)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AddInventoryItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productsList[position])
    }

    override fun getItemCount(): Int {
        return productsList.size
    }

    // Function to fetch data from Firebase and update the list
    fun fetchData() {
        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                productsList.clear()
                for (snapshot in dataSnapshot.children) {
                    val product = snapshot.getValue(RetrieveItem::class.java)
                    product?.let { productsList.add(it) }
                }
                notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    fun getItems(): List<RetrieveItem> {
        return productsList
    }
}
