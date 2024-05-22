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

class InventoryAdapter(private val context: Context) : RecyclerView.Adapter<InventoryAdapter.ViewHolder>() {

    private val database = FirebaseDatabase.getInstance()
    private val currentUserID: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val productsRef = database.getReference("products").child(currentUserID)
    // Data list for the products
    private var productsList: MutableList<RetrieveItem> = mutableListOf()
    private var productsList1: MutableList<DiscountItem> = mutableListOf()

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

        fun bind(retrieveItem: RetrieveItem?, discountItem: DiscountItem?) {
            retrieveItem?.let { product ->
                binding.itemName.text = product.foodName
                binding.SkuID.text = product.key
                binding.itemPrice.text = product.foodPrice.toString()
                binding.itemCategory.text = product.category.toString()
                binding.quantity.text = product.quantity

                Glide.with(context)
                    .load(product.foodImage)
                    .into(binding.ProductImage)

                binding.stock.text = product.stock // Update stock status here
            }

            discountItem?.let { discount ->
                binding.itemName.text = discount.foodNames
                binding.SkuID.text = discount.key
                binding.itemPrice.text = discount.foodPrices.toString()
                binding.itemCategory.text = discount.categorys.toString()
                binding.quantity.text = discount.quantitys

                Glide.with(context)
                    .load(discount.foodImages)
                    .into(binding.ProductImage)

                binding.stock.text = discount.stocks
                // Update other views as needed
            }
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
            val product: Any? = if (position < productsList.size) {
                // Regular product (RetrieveItem)
                productsList[position]
            } else {
                // Discount item (DiscountItem)
                val discountItemPosition = position - productsList.size
                productsList1.getOrNull(discountItemPosition)
            }

            if (product is RetrieveItem) {
                // Update the local list
                product.stock = stockStatus
                notifyItemChanged(position)

                // Update Firebase
                val ref = productsRef.child(product.key ?: "")
                ref.child("stock").setValue(stockStatus)
            } else if (product is DiscountItem) {
                // Update the local list
                product.stocks = stockStatus
                notifyItemChanged(position)

                // Update Firebase
                val ref = database.getReference("Discount-items").child(currentUserID)
                    .child(product.key ?: "")
                ref.child("stock").setValue(stockStatus)
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
            // Bind item from productsList
            val retrieveItem = productsList[position]
            holder.bind(retrieveItem, null)
        } else {
            // Bind item from productsList1
            val discountItemPosition = position - productsList.size
            val discountItem = productsList1.getOrNull(discountItemPosition)
            holder.bind(null, discountItem)
        }
    }



    override fun getItemCount(): Int {
        return productsList.size + productsList1.size
    }

    fun fetchData() {
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

        // Fetch RetrieveItem items
        val productsRef = currentUserID?.let { database.getReference("products").child(currentUserID) }
        if (productsRef != null) {
            productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.d("InventoryAdapter", "onDataChange called")
                    productsList.clear()
                    for (snapshot in dataSnapshot.children) {
                        val product = snapshot.getValue(RetrieveItem::class.java)
                        product?.let {
                            // Only add products belonging to the current user's admin ID
                            if (it.adminId == currentUserID) {
                                productsList.add(it)
                                Log.d("InventoryAdapter", "Added product: ${it.foodName}")
                            }
                        }
                    }
                    notifyDataSetChanged()
                }


                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })
        }

        // Fetch DiscountItem items from correct location (discount_items)
        val discountItemsRef = currentUserID?.let { database.getReference("Discount-items").child(it) }
        if (discountItemsRef != null) {
            discountItemsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    productsList1.clear()
                    for (snapshot in dataSnapshot.children) {
                        val discountItem = snapshot.getValue(DiscountItem::class.java)
                        discountItem?.let {
                            // Only add discount items belonging to the current user's admin ID
                            if (it.adminId == currentUserID) {
                                productsList1.add(it)
                            }
                        }
                    }
                    notifyDataSetChanged()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle error
                }
            })
        }
    }


    fun getItems(): List<RetrieveItem> {
        return productsList
    }

    fun getDiscountItems():  List<DiscountItem>  {
          return productsList1
    }
}
