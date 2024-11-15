package com.capztone.admin.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.capztone.admin.databinding.DltBinding
import com.capztone.admin.models.DeletedAccount

class DeleteAdapter(
    private val deletedAccounts: List<DeletedAccount>,
    private val onSendSMSClick: (DeletedAccount) -> Unit // Callback for send SMS button click
) : RecyclerView.Adapter<DeleteAdapter.DeletedAccountsViewHolder>() {

    inner class DeletedAccountsViewHolder(private val binding: DltBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(deletedAccount: DeletedAccount) {
            binding.tvContactInfo.text = deletedAccount.contactInfo
            binding.tvUserId.text = deletedAccount.userId
            binding.useridusers.text = deletedAccount.userUid

            // Handle send SMS button click
            binding.sendmail.setOnClickListener {
                onSendSMSClick(deletedAccount)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletedAccountsViewHolder {
        val binding = DltBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeletedAccountsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeletedAccountsViewHolder, position: Int) {
        holder.bind(deletedAccounts[position])
    }

    override fun getItemCount(): Int = deletedAccounts.size
}
