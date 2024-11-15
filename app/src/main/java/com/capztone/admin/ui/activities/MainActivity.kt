package com.capztone.admin.ui.activities

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import androidx.recyclerview.widget.LinearLayoutManager
import com.capztone.admin.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.capztone.admin.R
import com.capztone.admin.adapters.GeneralCategoryAdapter1
import com.capztone.admin.models.Category
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

import com.google.android.gms.common.api.GoogleApiClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var adminRef: DatabaseReference
    private lateinit var generalcategoryAdapter1: GeneralCategoryAdapter1
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var database: DatabaseReference
    private lateinit var shopNames: List<String>
    private lateinit var storage: FirebaseStorage
    private var isEditing = false  // Flag to check if the shop name is being edited
    private var previousShopName: String? = null  // Variable to store previous shop name
    private val categoryList = mutableListOf<Category>()
    private lateinit var deliveryDetailsRef: DatabaseReference
    private lateinit var shopNamesRef: DatabaseReference
    private lateinit var currentUserId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Categories")
        storage = FirebaseStorage.getInstance()

        disableSearchViewInput(binding.searchView1)

        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }
        // Show loading indicator
        binding.progress.visibility = View.VISIBLE


// Start a delay to hide the loading indicator after 1500 milliseconds (1.5 seconds)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.progress.visibility = View.GONE
            // Call your method to retrieve cart items or perform other operations
        }, 1500)

        binding.categories.visibility=View.GONE


        binding.menu.setOnClickListener {
            // Create a PopupMenu
            val popupMenu = PopupMenu(this, binding.menu)

            popupMenu.menu.add("Add New Shop")
            popupMenu.menu.add("Shop Profile")
            popupMenu.menu.add("Shop Settings")
            popupMenu.menu.add("Logout")

            // Set a listener for menu item clicks
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title) {
                    // Assuming you have set up View Binding for this Activity
// and have initialized binding as your binding instance

                    "Shop Settings" -> {
                        val shopName = binding.shopname.text.toString()

                        // Check if shopName has text before navigating
                        if (shopName.isNotEmpty()) {
                            // Create an Intent to navigate to ShopSettings activity with shopName as an extra
                            val intent = Intent(this, ShopSettings::class.java).apply {
                                putExtra("shopName", shopName)
                            }
                            startActivity(intent)
                        } else {
                            // Show a message if shop name is missing
                            Toast.makeText(this, "Please enter a shop name before proceeding", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }

                    "Logout" -> {
                        showLogoutConfirmationDialog()  // Call your logout function
                        true
                    }
                    "Add New Shop" -> {
                        showAddShopDialog()
                        true
                    }
                    "Shop Profile" -> {
                        startActivity(Intent(this, ShopDetailsGeneral::class.java))
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()  // Show the popup menu
        }
        // Initialize Firebase references
        deliveryDetailsRef = FirebaseDatabase.getInstance().getReference("Delivery Details")
        shopNamesRef = FirebaseDatabase.getInstance().getReference("ShopNames")

        fetchShopIdAndShopName()


        // Setup onClickListeners for buttons
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)


        binding.cardView788.setOnClickListener {
            startActivity(Intent(this, DeleteConfirmationActivity::class.java))
        }
        binding.allItemMenu.setOnClickListener {
            startActivity(Intent(this, AllItemsActivity::class.java))
        }
        binding.bannerCardview.setOnClickListener {
            startActivity(Intent(this, AddBannerActivity::class.java))
        }

        binding.cardView77.setOnClickListener {
            startActivity(Intent(this, Inventory::class.java))
        }
        binding.category.setOnClickListener {
            startActivity(Intent(this, GeneralAddCategoryActivity::class.java))
        }
        binding.cardView78.setOnClickListener {
            startActivity(Intent(this, OrderDetailsActivity::class.java))
        }

        binding.recyclerView.layoutManager =  LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        generalcategoryAdapter1 = GeneralCategoryAdapter1(categoryList, this) // Pass the listener
        binding.recyclerView.adapter = generalcategoryAdapter1


        // Fetch counts from Firebase and update TextViews
        fetchShopNameAndCategories()
        fetchShopNameAndCategories1()
    }
    private fun showAddShopDialog() {
        // Inflate the custom dialog layout
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_add_shop, null) // Assuming your XML is named dialog_add_shop.xml

        // Access the views from the inflated layout
        val editTextShopName = dialogView.findViewById<EditText>(R.id.editTextShopName)
        val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextPassword)
        val passwordToggle = dialogView.findViewById<ImageView>(R.id.passwordToggle)
        val editTextShopMail = dialogView.findViewById<EditText>(R.id.shopMail)
        val editTextShopOwnerName = dialogView.findViewById<EditText>(R.id.shopOwnerName)
        val editTextShopMobile = dialogView.findViewById<EditText>(R.id.shopMobile)
        val editTextShopCity = dialogView.findViewById<EditText>(R.id.shopCity)

        // Toggle password visibility when the toggle icon is clicked
        var isPasswordVisible = false
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            editTextPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordToggle.setImageResource(if (isPasswordVisible) R.drawable.visible else R.drawable.invisible) // Change the icon
            // Move cursor to the end of the text
            editTextPassword.setSelection(editTextPassword.text.length)
        }

        // Create the dialog with a transparent background
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)  // Set the custom view
            .setCancelable(true)
            .create()

        // Show the dialog first, then remove the default background
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Set the background to transparent
        dialog.show()

        // Set custom behavior for the positive button after showing the dialog
        dialog.findViewById<Button>(R.id.btnDialogYes)?.setOnClickListener {
            val shopName = editTextShopName.text.toString().trim()
            val password = editTextPassword.text.toString().trim()
            val mailId = editTextShopMail.text.toString().trim()
            val ownerName = editTextShopOwnerName.text.toString().trim()
            val mobile = editTextShopMobile.text.toString().trim()
            val city = editTextShopCity.text.toString().trim()

            if (shopName.isNotEmpty() && password.isNotEmpty() && mailId.isNotEmpty() && ownerName.isNotEmpty() && mobile.isNotEmpty() && city.isNotEmpty()) {
                // Check if the password exists before closing the dialog
                checkPasswordInFirebase(shopName, mailId, password, dialog, ownerName, mobile, city)
            } else {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_LONG).show()
            }
        }

        dialog.findViewById<ImageView>(R.id.cancel)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btnDialogNo)?.setOnClickListener {
            dialog.dismiss()
        }
    }
    private fun checkPasswordInFirebase(
        shopName: String,
        shopMail: String,
        password: String,
        dialog: AlertDialog,
        ownerName: String,
        mobile: String,
        city: String
    ) {
        val databaseRef = FirebaseDatabase.getInstance().getReference("ShopNames")

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var passwordExists = false
                var emailExists = false
                for (shopSnapshot in dataSnapshot.children) {
                    val storedPassword = shopSnapshot.child("password").getValue(String::class.java)
                    val storedMail = shopSnapshot.child("shopMailId").getValue(String::class.java)
                    if (storedPassword == password) {
                        passwordExists = true
                    }
                    if (storedMail == shopMail) {
                        emailExists = true
                    }
                    // Exit loop early if both conditions are met
                    if (passwordExists && emailExists) break
                }

                when {
                    passwordExists -> {
                        Toast.makeText(applicationContext, "Try another password, this one already exists", Toast.LENGTH_LONG).show()
                    }
                    emailExists -> {
                        Toast.makeText(applicationContext, "Try another email, this one already exists", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        saveShopNameToFirebase(shopName, shopMail, password, dialog, mobile, ownerName, city)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(applicationContext, "Error: ${databaseError.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
    private fun saveShopNameToFirebase(
        shopName: String,
        shopMail: String,
        password: String,
        dialog: AlertDialog,
        ownerName: String,
        mobile: String,
        city: String
    ) {
        // Get the last shop ID from Firebase and increment it
        val shopRef = FirebaseDatabase.getInstance().getReference("ShopNames") // Ensure this reference is correct
        shopRef.orderByKey().limitToLast(1).get().addOnSuccessListener { dataSnapshot ->
            var lastShopId = "SHOP10000" // Default starting ID if there are no shops yet
            val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
            val username = account?.displayName ?: "Unknown User"

            // Get the last shop ID, if available
            if (dataSnapshot.children.any()) {
                val lastShopKey = dataSnapshot.children.first().key
                lastShopId = lastShopKey ?: "SHOP10000"
            }

            // Increment the shop ID
            val newShopId = incrementShopId(lastShopId)

            // Get current date and time
            val currentDate = Date()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()) // Format for date

            val createdDate = dateFormat.format(currentDate) // Store date as a string

            // Create a new Shop entry
            val shopData = mapOf(
                "shopName" to shopName,
                "shopMailId" to shopMail,
                "password" to password,
                "mobileNumber" to mobile,
                "ownerName" to ownerName,
                "address" to city,
                "CreatedDate" to createdDate, // Store created date
                "CreatedBy" to username // Store Google login username
            )

            // Store the shop under the new shop ID
            shopRef.child(newShopId).setValue(shopData)
                .addOnSuccessListener {
                    // Handle success
                    Toast.makeText(this, "Shop added with ID $newShopId", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, ShopSettings::class.java).apply {
                        putExtra("shopName", shopName)
                    }
                    startActivity(intent) // Dismiss dialog only after success
                }
                .addOnFailureListener { e ->
                    // Handle failure
                    Toast.makeText(this, "Failed to add shop: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener { e ->
            // Handle failure to retrieve the last shop ID
            Toast.makeText(this, "Failed to retrieve last shop ID: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun incrementShopId(lastShopId: String): String {
        // Extract the numeric part from the last shop ID and increment it
        val numericPart = lastShopId.replace("SHOP", "").toIntOrNull() ?: 10000
        val newNumericPart = numericPart + 1
        return "SHOP$newNumericPart"
    }
    private fun disableSearchViewInput(searchView: androidx.appcompat.widget.SearchView) {
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchView.clearFocus()
                startActivity(Intent(this, ShopSearchActivity::class.java))

            }
        }
    }
    private fun fetchShopNameAndCategories() {
        val adminsRef = FirebaseDatabase.getInstance().getReference("Delivery Details")

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
                Toast.makeText(this@MainActivity, "Failed to load shop ID", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun fetchShopNameAndCategories1() {
        val adminsRef = FirebaseDatabase.getInstance().getReference("Delivery Details")

        // Change the reference to "Shop ID"
        adminsRef.child("Shop Id").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shopId = snapshot.getValue(String::class.java)
                if (!shopId.isNullOrEmpty()) {
                    // Fetch the categories using the retrieved shopId
                    fetchCategoriesFromFirebase(shopId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load shop ID", Toast.LENGTH_SHORT).show()
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

                generalcategoryAdapter1.updateShopName(shopId)
                generalcategoryAdapter1.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun fetchShopIdAndShopName() {
        // Step 1: Fetch Shop ID from Delivery Details path
        val shopIdRef = deliveryDetailsRef.child("Shop Id") // Adjust this path to where Shop ID is stored

        shopIdRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Get the Shop ID
                    val shopId = snapshot.getValue(String::class.java) // This is the Shop ID (e.g., "SHOP10001")

                    if (shopId != null) {
                        // Step 2: Use the Shop ID to fetch the shop name from ShopNames path
                        fetchShopNameUsingShopId(shopId)
                    } else {
                        Toast.makeText(this@MainActivity, "Shop ID not found in Delivery details", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "No Shop ID found in Delivery details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to fetch Shop ID: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Step 3: Fetch the shop name using the Shop ID from the ShopNames path
    private fun fetchShopNameUsingShopId(shopId: String) {
        val shopRef = shopNamesRef.child(shopId) // Reference to ShopNames using Shop ID

        shopRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Get the shop name
                    val shopName = snapshot.child("shopName").getValue(String::class.java)

                    if (shopName != null) {
                        // Set the shop name in the EditText
                        binding.shopname.setText(shopName)

                    } else {
                        Toast.makeText(this@MainActivity, "Shop name not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Shop ID not found in ShopNames", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to fetch shop name: ${error.message}", Toast.LENGTH_SHORT).show()
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
    private fun logout() {
        googleSignInClient.signOut().addOnCompleteListener(this, OnCompleteListener {
            // After signing out, move to login activity
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        })
    }
    override fun onBackPressed() {
        super.onBackPressed()
      finish()
    }
}