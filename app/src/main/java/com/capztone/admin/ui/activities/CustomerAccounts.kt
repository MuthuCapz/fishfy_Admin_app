package com.capztone.admin.ui.activities

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.adapters.CustomerAcAdapter
import com.capztone.admin.databinding.ActivityCustomerAccountsBinding
import com.capztone.admin.models.Customer
import com.google.firebase.database.*

class CustomerAccounts : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerAccountsBinding
    private lateinit var customerAdapter: CustomerAcAdapter
    private val customerList = mutableListOf<Customer>()
    private val filteredCustomerList = mutableListOf<Customer>() // List for filtered data

    private var activeCount = 0
    private var inactiveCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Show loading indicator
        binding.progress.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE
        }, 1800)

        // Initialize RecyclerView with customerList
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        customerAdapter = CustomerAcAdapter(customerList)
        binding.recyclerView.adapter = customerAdapter

        // Set up search functionality and fetch data after initializing the adapter
        setupSearchView()
        fetchCustomerData()

        // Handle back button click
        binding.backButton.setOnClickListener {
            finish()
        }

    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterCustomers(newText.orEmpty())
                return true
            }
        })
    }

    private fun filterCustomers(query: String) {
        val searchQuery = query.lowercase()
        filteredCustomerList.clear()

        if (searchQuery.isEmpty()) {
            // Show all customers if the query is empty
            filteredCustomerList.addAll(customerList)
        } else {
            customerList.forEach { customer ->
                val uid = customer.userid?.lowercase() ?: ""
                val email = customer.email?.lowercase() ?: ""
                val mobileNumber = customer.phoneNumber?.lowercase() ?: ""

                if (uid.contains(searchQuery) || email.contains(searchQuery) || mobileNumber.contains(searchQuery)) {
                    filteredCustomerList.add(customer)
                }
            }
        }

        // Update the adapter data
        customerAdapter.updateData(filteredCustomerList)
    }

    private fun fetchCustomerData() {
        val databaseRef = FirebaseDatabase.getInstance().getReference("users")

        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                customerList.clear()
                activeCount = 0
                inactiveCount = 0

                for (userSnapshot in snapshot.children) {
                    val uid = userSnapshot.key ?: continue
                    val customer = userSnapshot.getValue(Customer::class.java)
                    customer?.let {
                        it.uid = uid
                        customerList.add(it)

                        val userCount = snapshot.childrenCount
                        binding.userCount.text = "$userCount"

                        if (it.Status == "active") {
                            activeCount++
                        } else if (it.Status == "inactive") {
                            inactiveCount++
                        } else {

                        }
                    }
                }

                // Notify the adapter to show the full customer list initially
                customerAdapter.updateData(customerList)

                binding.activeCount.text = "$activeCount"
                binding.InActiveCount.text = "$inactiveCount"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CustomerActivity", "Error fetching data", error.toException())
            }
        })
    }
}