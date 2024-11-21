package com.capztone.admin.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capztone.admin.R
import com.capztone.admin.adapters.DeleteAdapter
import com.capztone.admin.databinding.ActivityDeleteConfirmationBinding
import com.capztone.admin.models.DeletedAccount
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DeleteConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDeleteConfirmationBinding
    private lateinit var adapter: DeleteAdapter
    private val deletedAccounts = mutableListOf<DeletedAccount>()
    private var currentDeletedAccount: DeletedAccount? = null
    private val EMAIL_REQUEST_CODE = 1001
    private val SMS_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeleteConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize adapter with the callback
        adapter = DeleteAdapter(deletedAccounts) { deletedAccount ->
            currentDeletedAccount = deletedAccount
            sendDeletionMessage(this,deletedAccount.contactInfo, deletedAccount.userId)
        }


        // Finish Activity
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        fetchDeletedAccounts()
    }
    private fun fetchDeletedAccounts() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("DeletedAccounts")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                deletedAccounts.clear()
                var hasDeletedAccounts = false

                for (dataSnapshot in snapshot.children) {
                    val userId = dataSnapshot.key ?: ""
                    val email = dataSnapshot.child("email").getValue(String::class.java)
                    val phoneNumber = dataSnapshot.child("phoneNumber").getValue(String::class.java)
                    val userUid = dataSnapshot.child("userUid").getValue(String::class.java)

                    val contactInfo = email?.takeIf { it.isNotBlank() } ?: phoneNumber ?: "N/A"

                    val delAdminReference = FirebaseDatabase.getInstance().getReference("DelAdmin")
                    delAdminReference.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(delAdminSnapshot: DataSnapshot) {
                            if (!delAdminSnapshot.exists()) {
                                val deletedAccount =
                                    userUid?.let { DeletedAccount(userId, contactInfo, it) }
                                if (deletedAccount != null) {
                                    deletedAccounts.add(deletedAccount)
                                }
                                hasDeletedAccounts = true
                                adapter.notifyDataSetChanged() // Notify adapter
                            }
                            updateUI(hasDeletedAccounts) // Update UI after checking each account
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                this@DeleteConfirmationActivity,
                                "Failed to check DelAdmin data",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }

                // If there are no children in the snapshot
                if (!snapshot.hasChildren()) {
                    updateUI(false) // No deleted accounts found
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DeleteConfirmationActivity,
                    "Failed to load data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun updateUI(hasDeletedAccounts: Boolean) {
        val noDeletedAccountsMessage: TextView = findViewById(R.id.no_deleted_accounts_message)
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)

        if (hasDeletedAccounts) {
            noDeletedAccountsMessage.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            noDeletedAccountsMessage.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
    }


    private fun sendDeletionMessage(context: Context, contactInfo: String, userId: String) {
        if (contactInfo.contains("@")) { // Simple check to determine if it's an email address
            sendDeletionEmail(context, contactInfo, userId)
        } else {
            sendSMS(context, contactInfo, userId)
        }
    }

    private fun sendDeletionEmail(context: Context, contactInfo: String, userId: String) {
        val emailBody = """
Dear User,

We are writing to inform you that your account has been deactivated as per your request. Please note that your data will be permanently deleted in 30 days.

If you have any questions or require further assistance, please feel free to contact our support team.

Thank you for using our services.

Best regards,
Fishfy
www.capztone.com
    """.trimIndent()

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(contactInfo))
            putExtra(Intent.EXTRA_SUBJECT, "Account Deactivation Confirmation")
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }

        try {
            startActivityForResult(Intent.createChooser(emailIntent, "Send Email"), EMAIL_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send email", Toast.LENGTH_SHORT).show()
        }
    }


    private fun sendSMS(context: Context, phoneNumber: String, userId: String) {
        val smsBody = """
Dear User,

We are writing to inform you that your account has been deactivated as per your request. Please note that your data will be permanently deleted in 30 days.

If you have any questions or require further assistance, please feel free to contact our support team.

Thank you for using our services.

Best regards,
Fishfy
www.capztone.com
    """.trimIndent()

        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$phoneNumber") // Only SMS apps should handle this
            putExtra("sms_body", smsBody)
        }

        try {
            startActivityForResult(smsIntent, SMS_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show()
        }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EMAIL_REQUEST_CODE || requestCode == SMS_REQUEST_CODE) {
            currentDeletedAccount?.let { deletedAccount ->
                showSentConfirmationDialog(deletedAccount.userId)
            }
        }
    }

    private fun showSentConfirmationDialog(userId: String) {
        AlertDialog.Builder(this)
            .setTitle("Message Sent?")
            .setMessage("Did you successfully send the message?")
            .setPositiveButton("Yes") { _, _ -> removeDeletedAccount(userId) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun removeDeletedAccount(userId: String) {
        val delAdminReference = FirebaseDatabase.getInstance().getReference("DelAdmin")

        val deletedAccount = deletedAccounts.find { it.userId == userId }

        deletedAccount?.let { account ->
            // Add the deleted account to "DelAdmin" node
            delAdminReference.child(userId).setValue(account).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account successfully moved to DelAdmin", Toast.LENGTH_SHORT).show()
                    deletedAccounts.removeAll { it.userId == userId }
                    adapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this, "Failed to move account to DelAdmin", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
