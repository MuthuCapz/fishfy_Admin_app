package com.capztone.admin.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.capztone.admin.R
import com.capztone.admin.databinding.AllCustomersItemBinding
import com.capztone.admin.models.Customer
import com.capztone.admin.ui.activities.CustomerAccountDetails
import com.capztone.admin.ui.activities.CustomerAccounts

class CustomerAcAdapter(
    private var customerList: List<Customer>
) : RecyclerView.Adapter<CustomerAcAdapter.CustomerViewHolder>() {

    inner class CustomerViewHolder(private val binding: AllCustomersItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customer: Customer) {
            binding.userName.text = customer.userid
            binding.mailOrMobile.text = customer.email ?: customer.phoneNumber ?: "No contact info"
            binding.authId.text = customer.uid

            Glide.with(binding.userImage.context)
                .load(customer.profileImage)
                .placeholder(R.drawable.pic_default) // Replace with your default image resource
                .into(binding.userImage)

            // Set status indicator color based on the customer status
            when (customer.Status) {
                "active" -> {
                    binding.statusIndicator.setBackgroundResource(R.drawable.circle_status_indicator_active) // Green status
                }
                "inactive" -> {
                    binding.statusIndicator.setBackgroundResource(R.drawable.circle_status_indicator_inactive) // Grey status
                }
                else -> {
                    binding.statusIndicator.setBackgroundResource(R.drawable.circle_status_indicator_inactive) // Default indicator
                }
            }
            // Set OnClickListener to pass userId to CustomerDetailsActivity
            binding.root.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, CustomerAccountDetails::class.java).apply {
                    putExtra("USER_ID", customer.uid) // Pass userId
                    putExtra("USER_UID", customer.userid)
                }
                context.startActivity(intent)
            }


        }
    }
    // Add a function to update the data
    fun updateData(newList: MutableList<Customer>) {
        customerList = newList
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = AllCustomersItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(customerList[position])
    }

    override fun getItemCount(): Int = customerList.size
}
