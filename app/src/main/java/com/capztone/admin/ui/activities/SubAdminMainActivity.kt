package com.capztone.admin.ui.activities
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.capztone.admin.R
import com.capztone.admin.adapters.CategoryAdapter1
import com.capztone.admin.databinding.ActivitySubAdminMainBinding
import com.capztone.admin.models.Category
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener



class SubAdminMainActivity : AppCompatActivity() {

    private lateinit var binding:  ActivitySubAdminMainBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var categoryAdapter1: CategoryAdapter1
    private val categoryList = mutableListOf<Category>()
    private lateinit var database: FirebaseDatabase
    private lateinit var currentUserId: String





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // Show loading indicator
        binding.progress.visibility = View.VISIBLE


// Start a delay to hide the loading indicator after 1500 milliseconds (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE
            // Call your method to retrieve cart items or perform other operations
        }, 1500)
        // Firebase database instance
        database = FirebaseDatabase.getInstance()

        // Get current user ID from FirebaseAuth
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""


        // Setup onClickListeners for buttons
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        loadProfileImage()
        binding.SubInventory.setOnClickListener {
            startActivity(Intent(this,SubAdminInventory::class.java))
        }
        binding.New.setOnClickListener {
            // Get the text from the new_orders2 TextView
            val textToPass = binding.newOrders2.text.toString()

            // Create an intent to start SubAdminOrderdetails activity
            val intent = Intent(this, SubAdminOrderdetails::class.java)

            // Add the text as an extra
            intent.putExtra("orderText", textToPass)

            // Start the activity
            startActivity(intent)
        }

        binding.New2.setOnClickListener {
            // Get the text from the new_orders2 TextView
            val textToPass = binding.ongoing3.text.toString()

            // Create an intent to start SubAdminOrderdetails activity
            val intent = Intent(this, SubAdminOrderdetails::class.java)

            // Add the text as an extra
            intent.putExtra("orderText", textToPass)

            // Start the activity
            startActivity(intent)
        }

        binding.New3.setOnClickListener {
            // Get the text from the new_orders2 TextView
            val textToPass = binding.button1.text.toString()

            // Create an intent to start SubAdminOrderdetails activity
            val intent = Intent(this, SubAdminOrderdetails::class.java)

            // Add the text as an extra
            intent.putExtra("orderText", textToPass)

            // Start the activity
            startActivity(intent)
        }

        binding.New4.setOnClickListener {
            // Get the text from the new_orders2 TextView
            val textToPass = binding.button1Text.text.toString()

            // Create an intent to start SubAdminOrderdetails activity
            val intent = Intent(this, SubAdminOrderdetails::class.java)

            // Add the text as an extra
            intent.putExtra("orderText", textToPass)

            // Start the activity
            startActivity(intent)
        }
        binding.AllProducts.setOnClickListener {

            startActivity(Intent(this, SubAdminAllItemsActivity::class.java))
        }
        binding.SubInventory.setOnClickListener {

            startActivity(Intent(this, SubAdminInventory::class.java))
        }
        binding.addCategory.setOnClickListener {
            startActivity(Intent(this, AddCategoryActivity::class.java))
        }
        binding.profileImage.setOnClickListener{
            startActivity(Intent(this, ProfileDetailsOwner::class.java))
        }
        binding.recyclerView.layoutManager =  LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        categoryAdapter1 = CategoryAdapter1(categoryList, this) // Pass the listener
        binding.recyclerView.adapter = categoryAdapter1
        retrieveShopName()
        fetchShopNameAndCategories()
        updateOrderCounts()
        binding.logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }


    }
    override fun onResume() {
        super.onResume()
        retrieveShopName()
    }

    private fun retrieveShopName() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val database = FirebaseDatabase.getInstance().reference

        if (currentUserId != null) {
            // Step 1: Retrieve Shop ID from Admins path and listen for changes
            val shopIdRef = database.child("Admins").child(currentUserId).child("Shop Id")
            shopIdRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(shopIdSnapshot: DataSnapshot) {
                    val shopId = shopIdSnapshot.value as? String
                    if (shopId != null) {
                        // Step 2: Retrieve Shop Name from Shops path using Shop ID and listen for changes
                        val shopNameRef = database.child("ShopNames").child(shopId).child("shopName")
                        shopNameRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(shopNameSnapshot: DataSnapshot) {
                                val shopName = shopNameSnapshot.value as? String
                                if (shopName != null) {
                                    // Step 3: Immediate UI update - Ensure UI update on the main thread
                                    binding.textView50.text = shopName
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.e("DatabaseError", "Failed to retrieve Shop Name: ${error.message}")
                            }
                        })
                    } else {
                        Log.e("ShopId", "Shop ID not found.")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DatabaseError", "Failed to retrieve Shop ID: ${error.message}")
                }
            })
        }
    }



    private fun updateOrderStatus(status: String) {
        if (currentUserId.isNotEmpty()) {
            val orderStatusRef = database.getReference("Admins").child(currentUserId).child("OrderStatus")
            orderStatusRef.setValue(status).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Order status updated to $status", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to update order status", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadProfileImage() {
        // Get the last signed-in Google account
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)

        // If an account is signed in, load the profile image
        account?.let {
            val personPhoto: Uri? = it.photoUrl
            if (personPhoto != null) {
                // Load the profile image using Glide
                Glide.with(this)
                    .load(personPhoto)
                    .placeholder(R.drawable.user_icon)  // Placeholder image
                    .into(binding.profileImage)  // profileImage is the id of your CircleImageView
            } else {
                // Set a default image if no profile photo is found
                binding.profileImage.setImageResource(R.drawable.user_icon)
            }
        }
    }
    private fun updateOrderCounts() {
        if (currentUserId.isNotEmpty()) {
            // Reference to the shop name of the current admin
            val adminShopNameRef = database.getReference("Admins").child(currentUserId).child("Shop Id")

            // Retrieve the shop name for the current user (admin)
            adminShopNameRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(adminSnapshot: DataSnapshot) {
                    val adminShopName = adminSnapshot.getValue(String::class.java) ?: ""

                    if (adminShopName.isNotEmpty()) {
                        // Reference to order details
                        val orderDetailsRef = database.getReference("OrderDetails")

                        // Retrieve all orders
                        orderDetailsRef.addValueEventListener(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                var newOrderCount = 0
                                var completeCount = 0
                                var ongoingCount = 0
                                var cancelOrderCount = 0

                                for (orderSnapshot in dataSnapshot.children) {
                                    // Use GenericTypeIndicator to retrieve the shopNames list
                                    val shopNamesType = object : GenericTypeIndicator<List<String>>() {}
                                    val shopNames = orderSnapshot.child("shopNames").getValue(shopNamesType)
                                    val firstShopName = shopNames?.get(0).toString()

                                    // Compare shop name in the order with the current admin's shop name
                                    if (firstShopName == adminShopName) {
                                        // Retrieve order status details
                                        val status = orderSnapshot.child("Status").child("message").getValue(String::class.java) ?: ""
                                        val cancellationMessage = orderSnapshot.child("cancellationMessage").getValue(String::class.java) ?: ""

                                        // Check for new orders (no status and not yet accepted)
                                        if (status.isNullOrBlank()) {
                                            newOrderCount++
                                        }

                                        // Check for ongoing orders (accepted but not yet delivered)
                                        if (status == "Order accepted" || status == "Order picked") {
                                            ongoingCount++
                                        }

                                        // Check for completed orders (status is delivered)
                                        if (status == "Order delivered") {
                                            completeCount++
                                        }

                                        // Count cancellations (cancellation message is not empty)
                                        if (cancellationMessage.isNotEmpty()) {
                                            cancelOrderCount++
                                        }
                                    }
                                }

                                // Adjust newOrderCount to subtract cancelOrderCount
                                newOrderCount -= cancelOrderCount

                                // Update TextViews with the counts displayed in square brackets
                                binding.newOrders3.text = "[$newOrderCount]"
                                binding.completed3.text = "[$completeCount]"
                                binding.ongoing4.text = "[$ongoingCount]"
                                binding.canceled3.text = "[$cancelOrderCount]"

                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Toast.makeText(this@SubAdminMainActivity, "Failed to load order counts", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@SubAdminMainActivity, "Failed to load shop name", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchShopNameAndCategories() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val adminsRef = FirebaseDatabase.getInstance().getReference("Admins").child(currentUserId)

        // Change the reference to "Shop ID"
        adminsRef.child("Shop Id").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shopId = snapshot.getValue(String::class.java)
                if (!shopId.isNullOrEmpty()) {
                    // Fetch the categories using the retrieved shopId
                    fetchCategoriesFromFirebase(shopId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SubAdminMainActivity, "Failed to load shop ID", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun fetchCategoriesFromFirebase(shopId: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Categories").child(shopId)

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryList.clear()

                if (snapshot.exists()) {
                    for (categorySnapshot in snapshot.children) {
                        val name = categorySnapshot.key ?: ""
                        val image = categorySnapshot.child("image").getValue(String::class.java) ?: ""
                        val category = Category(name, image)
                        categoryList.add(category)
                    }

                    binding.categories.visibility = View.VISIBLE
                } else {
                    binding.categories.visibility = View.GONE
                }

                categoryAdapter1.updateShopName(shopId)
                categoryAdapter1.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SubAdminMainActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun showLogoutConfirmationDialog() {
        // Create a dialog instance
        val dialog = Dialog(this)
        // Inflate the custom layout
        val dialogView: View = LayoutInflater.from(this).inflate(R.layout.dialog_logout_confirmation, null)
        dialog.setContentView(dialogView)

        // Get references to the Yes and No buttons in the custom layout
        val btnDialogYes = dialogView.findViewById<Button>(R.id.btnDialogYes)
        val btnDialogNo = dialogView.findViewById<Button>(R.id.btnDialogNo)

        // Handle Yes button click
        btnDialogYes.setOnClickListener {
            logout()  // Call the logout function
            dialog.dismiss() // Close the dialog
        }

        // Handle No button click
        btnDialogNo.setOnClickListener {
            dialog.dismiss()  // Just close the dialog
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // Show the dialog

    }
    override fun onBackPressed() {
        super.onBackPressed()
        // This method closes the app when the back button is pressed in MainActivity
        finishAffinity()
    }

    private fun logout() {
        googleSignInClient.signOut().addOnCompleteListener(this, OnCompleteListener {
            // After signing out, move to login activity
            val intent = Intent(this@SubAdminMainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        })
    }

}