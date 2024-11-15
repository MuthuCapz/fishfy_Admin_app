package com.capztone.admin.ui.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.MenuInflater
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivitySubAdminProductEditBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubAdminProductEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubAdminProductEditBinding
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1
    private var foodImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubAdminProductEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup status bar and window appearance
        setupWindowAppearance()

        // Initialize Firebase Auth and Database reference
        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference

        // Retrieve data from the Intent
        val category = intent.getStringExtra("category")
        val skuId = intent.getStringExtra("skuId")
        val names = intent.getStringExtra("names")
        val prices = intent.getStringExtra("prices")
        val imageUri = intent.getStringExtra("image")
        val quantity = intent.getStringExtra("quantity")
        val productquantity = intent.getStringExtra("productquantity")
        val description = intent.getStringExtra("description")

        // Split names by comma and trim extra spaces
        val foodNamesList = names?.split(",")?.map { it.trim() } ?: listOf()

        // Extract the file name from the image URI if available
        val imageName = imageUri?.let {
            Uri.parse(it).lastPathSegment
        } ?: "Image Not Available"

        // Set the retrieved data to the appropriate views
        binding.apply {
            categoryTextView.text = category
            skuidEdit.setText(skuId)
            edittextFoodPrice.setText(prices)
            quantitycount.setText(quantity)
            productQuantity.setText(productquantity)
            editTextDescription.setText(description)

            editTextFoodName.setText(foodNamesList.getOrNull(0))
            editText1FoodName.setText(foodNamesList.getOrNull(1))
            editText2FoodName.setText(foodNamesList.getOrNull(2))
            editText3FoodName.setText(foodNamesList.getOrNull(3))

            // Display the file name in a TextView
            selectImage.text = imageName
        }

        // Retrieve and display the shop name
        retrieveShopName()

        // Set click listener on the update button
        binding.addItemButton.setOnClickListener {
            val shopName = binding.shopname.text.toString()
            updateItem(shopName, skuId)
        }
        binding.quantityoptions.setOnClickListener {
            showQuantityOptions(it)
        }
        binding.selectImage.setOnClickListener {
            openGallery()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            foodImageUri = data?.data // Set foodImageUri instead of selectedImageUri
            foodImageUri?.let { displayImage(it) }
        }
    }

    private fun displayImage(imageUri: Uri) {
        selectedImageUri = imageUri // Store the selected Uri
        val fileName = getFileName(imageUri)
        if (fileName != null) {
            binding.selectImage.text = fileName
        } else {
            binding.selectImage.text = imageUri.lastPathSegment // Fallback if filename retrieval fails
        }


    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun setupWindowAppearance() {
        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }
    }

    private fun retrieveShopName() {
        // Retrieve the shop name from Firebase
        val userId = auth.currentUser?.uid
        databaseRef.child("Admins/$userId/Shop Id").get()
            .addOnSuccessListener { dataSnapshot ->
                val shopName = dataSnapshot.getValue<String>()
                binding.shopname.text = shopName ?: "Shop Name Not Available"
            }
            .addOnFailureListener {
                binding.shopname.text = "Error retrieving shop name"
            }
    }

    private fun updateItem(shopName: String, skuId: String?) {
        if (skuId.isNullOrEmpty()) {
            Toast.makeText(this, "SKU ID is missing", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val username = account?.displayName ?: "Unknown User"
        val currentDate = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())

        val shopRef = databaseRef.child("Shops").child(shopName)

        shopRef.get().addOnSuccessListener { dataSnapshot ->
            var itemFound = false

            dataSnapshot.children.forEach { menuSnapshot ->
                val itemSnapshot = menuSnapshot.child(skuId)

                if (itemSnapshot.exists()) {
                    val updatedData = mapOf(
                        "category" to binding.categoryTextView.text.toString(),
                        "foodDescription" to binding.editTextDescription.text.toString(),
                        "foodImage" to intent.getStringExtra("image"),
                        "foodName" to mapOf(
                            "0" to binding.editTextFoodName.text.toString(),
                            "1" to binding.editText1FoodName.text.toString(),
                            "2" to binding.editText2FoodName.text.toString(),
                            "3" to binding.editText3FoodName.text.toString()
                        ),
                        "foodPrice" to binding.edittextFoodPrice.text.toString(),
                        "key" to skuId,
                        "productQuantity" to binding.productQuantity.text.toString(),
                        "quantity" to binding.quantitycount.text.toString(),
                        "updatedDate" to currentDate,
                        "updatedBy" to username
                    )

                    itemSnapshot.ref.updateChildren(updatedData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Item updated successfully", Toast.LENGTH_SHORT).show()
                            navigateToMainActivity()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to update item", Toast.LENGTH_SHORT).show()
                        }
                    itemFound = true
                    return@forEach
                }
            }

            if (!itemFound) {
                Toast.makeText(this, "SKU ID not found under any menu", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error fetching shop data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showQuantityOptions(view: View) {
        val popupMenu = PopupMenu(this, view)
        val menuInflater: MenuInflater = popupMenu.menuInflater
        menuInflater.inflate(R.menu.quanity_menu, popupMenu.menu)

        popupMenu.gravity = Gravity.NO_GRAVITY
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.option_10kg -> { binding.quantitycount.setText("10kg"); true }
                R.id.option_20kg -> { binding.quantitycount.setText("20kg"); true }
                R.id.option_500g -> { binding.quantitycount.setText("500g"); true }
                R.id.option_250g -> { binding.quantitycount.setText("250g"); true }
                R.id.option_50kg -> { binding.quantitycount.setText("50kg"); true }
                R.id.option_100kg -> { binding.quantitycount.setText("100kg"); true }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, SubAdminMainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
