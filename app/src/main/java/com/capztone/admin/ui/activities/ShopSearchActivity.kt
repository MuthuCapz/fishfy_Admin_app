package com.capztone.admin.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.adapters.ShopSearchAdapter
import com.capztone.admin.databinding.ActivityShopSearchBinding
import com.capztone.admin.models.SearchShop
import android.content.SharedPreferences
import com.google.firebase.database.*

class ShopSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShopSearchBinding
    private lateinit var shopDatabase: DatabaseReference // For ShopNames path
    private lateinit var deliveryDetailsDatabase: DatabaseReference // For Delivery Details path
    private lateinit var shopList: MutableList<SearchShop>
    private lateinit var filteredShopList: MutableList<SearchShop> // List for filtered shops
    private lateinit var shopSearchAdapter: ShopSearchAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private val RECENT_SEARCH_KEY = "recent_searches"
    private lateinit var recentSearches: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up status bar transparency

        // Show loading indicator
        binding.progress.visibility = View.VISIBLE
        // Start a delay to hide the loading indicator after 1500 milliseconds (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE
            // Call your method to retrieve cart items or perform other operations
        }, 1800)
        // Initialize Firebase Database references
        shopDatabase = FirebaseDatabase.getInstance().getReference("ShopNames")
        deliveryDetailsDatabase = FirebaseDatabase.getInstance().getReference("Delivery Details")

        // Initialize lists and adapter
        shopList = mutableListOf()
        filteredShopList = mutableListOf() // Initialize filtered list
        shopSearchAdapter = ShopSearchAdapter(filteredShopList) { shopId ->
            storeShopIdAndNavigate(shopId) // Pass the click event to this method
        }
        binding.shopRecycler.layoutManager = LinearLayoutManager(this)
        binding.shopRecycler.adapter = shopSearchAdapter




        // Handle back button click
        binding.searchBackButton.setOnClickListener {
            finish()
        }
        binding.image.visibility = View.VISIBLE
        // Set up search functionality

        sharedPreferences = getSharedPreferences("shop_search_prefs", MODE_PRIVATE)
        recentSearches = loadRecentSearches()


// In your SearchView setup in ShopSearchActivity's onCreate
        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                fetchShopData() // Fetch shop data when the SearchView is focused
                binding.image.visibility = View.GONE
                 // Optionally display recent searches
            }
        }

        // Handle text input in SearchView
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    saveRecentSearch(query)
                    filterShops(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterShops(newText)

                return true
            }
        })

        // Set up search close icon listener
        binding.searchView.setOnCloseListener {

            false
        }
        // Ensure only the cancel icon shows
        binding.searchView.setIconifiedByDefault(false) // Keeps the SearchView expanded by default
        binding.searchView.isSubmitButtonEnabled = false
    }



    private fun fetchShopData() {
        shopDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                shopList.clear() // Clear old data
                for (shopSnapshot in snapshot.children) {
                    val shopId = shopSnapshot.key // e.g., SHOP10001, SHOP10002

                    // Ensure you're accessing the correct path for shopName
                    val shopNameSnapshot = shopSnapshot.child("shopName")
                    val shopName = if (shopNameSnapshot.exists()) {
                        shopNameSnapshot.value as? String // Cast to String safely
                    } else {
                        null // Handle the case where shopName does not exist
                    }

                    Log.d("ShopSearchActivity", "Shop ID: $shopId, Shop Name: $shopName")

                    if (shopId != null && shopName != null) {
                        shopList.add(SearchShop(shopId, shopName)) // Create Shop object
                    }
                }

                // Initially show only the first 5 shops
                filteredShopList.clear()
                filteredShopList.addAll(shopList.take(5)) // Limit to first 5 items
                shopSearchAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ShopSearchActivity", "Database error: ${error.message}")
            }
        })
    }

    private fun loadRecentSearches(): MutableList<String> {
        val recent = sharedPreferences.getStringSet(RECENT_SEARCH_KEY, setOf()) ?: setOf()
        return recent.toMutableList()
    }

    private fun saveRecentSearch(query: String) {
        // Remove the search if it already exists to avoid duplicates
        if (query in recentSearches) {
            recentSearches.remove(query)
        }
        // Add the new search at the top
        recentSearches.add(0, query)
        // Limit to the latest 5 searches
        if (recentSearches.size > 5) {
            recentSearches = recentSearches.take(5).toMutableList()
        }
        // Save to SharedPreferences
        sharedPreferences.edit().putStringSet(RECENT_SEARCH_KEY, recentSearches.toSet()).apply()
    }

    // Save new search to recent searches, keeping only the latest 5


    private fun filterShops(query: String?) {
        filteredShopList.clear()
        if (query.isNullOrEmpty()) {
            filteredShopList.addAll(shopList) // Show all if query is empty
        } else {
            val lowerCaseQuery = query.lowercase()
            for (shop in shopList) {
                // Check if shop ID or shop name contains the query
                if (shop.shopId!!.lowercase().contains(lowerCaseQuery) || shop.shopName!!.lowercase().contains(lowerCaseQuery)) {
                    filteredShopList.add(shop)
                }
            }
        }

        // Show or hide noResultsTextView based on the results
        if (filteredShopList.isEmpty()) {
            binding.noResultsTextView.visibility = View.VISIBLE
        } else {
            binding.noResultsTextView.visibility = View.GONE
        }

        shopSearchAdapter.notifyDataSetChanged() // Notify adapter about data changes
    }


    private fun storeShopIdAndNavigate(shopId: String) {
        // Store the selected shopId in Delivery Details path
        deliveryDetailsDatabase.child("Shop Id").setValue(shopId)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ShopSearchActivity", "Shop Id $shopId stored successfully.")
                    // Navigate to MainActivity after storing the shopId
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Optionally finish the current activity
                } else {
                    Log.e("ShopSearchActivity", "Failed to store Shop ID: ${task.exception?.message}")
                }
            }
    }
}