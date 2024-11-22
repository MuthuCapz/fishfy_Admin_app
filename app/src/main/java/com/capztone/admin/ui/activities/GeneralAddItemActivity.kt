package com.capztone.admin.ui.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.MenuInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivityGeneralAddItemBinding
import com.capztone.admin.models.AllMenu
import com.capztone.admin.utils.FirebaseAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class GeneralAddItemActivity : AppCompatActivity() {

    // Food item Details
    private lateinit var foodNames: ArrayList<String>
    private lateinit var foodPrice: String
    private lateinit var foodDescription: String
    private lateinit var Quantity: String
    private lateinit var stock: String
    private lateinit var category: String
    private var PQuantity: String = "1 Kg"
    private var foodImageUri: Uri? = null
    private lateinit var categoryName: String
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImageUri: Uri? = null // Store the Uri of the selected image
    private val PICK_IMAGE_REQUEST = 1

    private val binding: ActivityGeneralAddItemBinding by lazy {
        ActivityGeneralAddItemBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        //Initialize Firebase
auth = FirebaseAuthUtil.auth
        // Initialize Firebase database Instance
        database = FirebaseDatabase.getInstance()


        categoryName = intent.getStringExtra("categoryName") ?: ""
        // Use categoryName as needed, e.g., display it in a TextView
        val categoryTextView: TextView = findViewById(R.id.category)
        categoryTextView.text = categoryName

        // Initialize food names array
        foodNames = ArrayList()

        // Finish Activity
        binding.backButton.setOnClickListener {
            finish()
        }
        // Access the views through binding object
        val scrollView = binding.scrollView

        // Set focus change listeners to scroll to the respective EditText when focused
        binding.editTextDescription.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.post {
                    scrollView.smoothScrollTo(0, binding.editTextDescription.bottom)
                }
            }
        }

// References to views
        val numberEditText = binding.productQuantity // EditText for quantity
        val incrementButton = binding.incrementButton
        val decrementButton = binding.decrementButton
        val unitTextView = binding.unit // AutoCompleteTextView for unit selection

// Variables for quantity management
        var currentNumber = 1 // Initial quantity value
        val maxNumber = 500 // Maximum value
        val minNumber = 1 // Minimum value

// Unit options for dropdown (AutoCompleteTextView)
        val units = listOf("Kg", "g") // Units: "Kg" is the default

// Create an ArrayAdapter for the unit dropdown
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, units)
        unitTextView.setAdapter(adapter)

        // Function to update the combined product quantity and unit
        fun updateProductQty() {
            val unit = unitTextView.text.toString()
            val qty=numberEditText.text.toString()
            val productQty = "$qty $unit"
            println("Updated productQty: $productQty")
            PQuantity=productQty// For debugging, log the updated value
        }

// Show dropdown when unit field is clicked
        unitTextView.setOnClickListener {
            unitTextView.showDropDown()
        }

// Listen for changes in the unit selection to update productQty
        unitTextView.setOnItemClickListener { _, _, _, _ ->
            updateProductQty()
        }

// Increment button functionality with validation for unit selection
        incrementButton.setOnClickListener {
            if (unitTextView.text.isNullOrBlank()) {
                // Show error if unit is not selected
                unitTextView.error = "Please select a unit"
            } else if (currentNumber < maxNumber) {
                currentNumber++
                numberEditText.setText(currentNumber.toString())
                updateProductQty() // Update productQty when quantity changes
            }
        }

// Decrement button functionality with validation for unit selection
        decrementButton.setOnClickListener {
            if (unitTextView.text.isNullOrBlank()) {
                // Show error if unit is not selected
                unitTextView.error = "Please select a unit"
            } else if (currentNumber > minNumber) {
                currentNumber--
                numberEditText.setText(currentNumber.toString())
                updateProductQty() // Update productQty when quantity changes
            }
        }


        // Validation to ensure quantity and unit are both set
        fun validateQuantityAndUnit(): Boolean {
            val quantity = numberEditText.text.toString().toIntOrNull()
            val unit = unitTextView.text.toString()

            return when {
                quantity == null || quantity < minNumber -> {
                    numberEditText.error = "Please enter a valid quantity"
                    false
                }
                unit.isBlank() -> {
                    unitTextView.error = "Please select a unit"
                    false
                }
                else -> true
            }
        }

        binding.quantityoptions.setOnClickListener {
            showQuantityOptions(it)
        }

        binding.quantity.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollView.post {
                    scrollView.smoothScrollTo(0, binding.quantity.bottom)
                }
            }
        }

        ///
        binding.addItemButton.setOnClickListener {
            // Call the validation function first
            if (!validateQuantityAndUnit()) {
                // If validation fails, stop further processing
                Toast.makeText(this, "Please enter a valid quantity and select a unit", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // GET Data from EditText fields
            foodPrice = binding.edittextFoodPrice.text.toString().trim()
            foodDescription = binding.editTextDescription.text.toString().trim()
            Quantity = binding.quantity.text.toString().trim()
            category = binding.category.text.toString().trim()

            // Clear previous entries
            foodNames.clear()

            // Retrieve food names from EditText fields
            for (i in 1..4) {
                val foodNameEditText = when (i) {
                    1 -> binding.editTextFoodName
                    2 -> binding.editText1FoodName
                    3 -> binding.editText2FoodName
                    4 -> binding.editText3FoodName
                    else -> binding.editText3FoodName
                }
                val foodName = foodNameEditText.text.toString().trim()


                if (foodName.isNotBlank()) {
                    foodNames.add(foodName)
                }
            }

            val userId = auth.currentUser?.uid

            // Check if all required fields are filled
            val english = binding.editTextFoodName.text.toString().trim()
            val tamil = binding.editText1FoodName.text.toString().trim()
            val telugu = binding.editText2FoodName.text.toString().trim()
            val malayalam = binding.editText3FoodName.text.toString().trim()
            if (foodNames.isNotEmpty() && english.isNotEmpty() && tamil.isNotEmpty() && telugu.isNotEmpty() && malayalam.isNotEmpty() && foodPrice.isNotBlank() && foodDescription.isNotBlank() && Quantity.isNotBlank() && category.isNotBlank() && PQuantity.isNotBlank()) {
                uploadData(userId)
                Toast.makeText(this, "Item Add Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Fill All the Details", Toast.LENGTH_SHORT).show()
            }
        }

        // Inside your onCreateView or onViewCreated
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

        // Clear the drawable if needed

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

    private fun showQuantityOptions(view: View) {
        // Create a PopupMenu
        val popupMenu = PopupMenu(this, view) // Attach to the view
        val menuInflater: MenuInflater = popupMenu.menuInflater
        menuInflater.inflate(R.menu.quanity_menu, popupMenu.menu) // Inflate your menu

        // Show the popup menu just below the TextView
        popupMenu.gravity = Gravity.NO_GRAVITY // Disable default positioning
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.option_10kg -> {
                    binding.quantity.setText("10kg")
                    true
                }

                R.id.option_20kg -> {
                    binding.quantity.setText("20kg")
                    true
                }



                R.id.option_50kg -> {
                    binding.quantity.setText("50kg")
                    true
                }

                R.id.option_100kg -> {
                    binding.quantity.setText("100kg")
                    true
                }

                else -> false
            }
        }
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        popupMenu.show() // Show the popup menu
        popupMenu.setOnDismissListener {
            // Optionally do something when the popup is dismissed
        }
    }

    private fun uploadData(userId: String?) {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val username = account?.displayName ?: "Unknown User"
        // Get current date and time
        val sdfDate = SimpleDateFormat("dd-MM-yyyy hh:mm a", java.util.Locale.getDefault())

        val currentDate = sdfDate.format(Date())


        // Get a reference to the "Delivery Details" node using the current user ID (auth ID)
        val adminRef = database.getReference("Delivery Details")

        // Fetch Shop ID
        adminRef.child("Shop Id").get().addOnSuccessListener { dataSnapshot ->
            val shopId = dataSnapshot.getValue(String::class.java)
            if (shopId != null) {
                val shopNameRef = database.getReference("ShopNames").child(shopId)

                shopNameRef.child("shopName").get().addOnSuccessListener { nameSnapshot ->
                    val shopName = nameSnapshot.getValue(String::class.java)
                    if (shopName != null) {
                        val categoryName = binding.category.text.toString().trim()

                        val newSKUPrefix = "SKU-"
                        val newSKU = newSKUPrefix + (100000 + Random.nextInt(900000))

                        if (foodImageUri != null) {
                            val storageRef = FirebaseStorage.getInstance().reference
                            val imageRef = storageRef.child("menu_images/${newSKU}.jpg")

                            imageRef.putFile(foodImageUri!!)
                                .addOnSuccessListener {
                                    imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                                        val newItem = AllMenu(
                                            newSKU,
                                            foodNames, // ArrayList of food names
                                            foodPrice,
                                            foodDescription,
                                            Quantity,
                                            downloadUrl.toString(),
                                            category,
                                            PQuantity,
                                            CreatedDate = currentDate,
                                            CreatedBy = username
                                        )

                                        val shopRef = database.getReference("Shops").child(shopId)
                                        shopRef.child(categoryName).child(newSKU).setValue(newItem)
                                            .addOnSuccessListener {
                                                shopRef.child("Shop name").setValue(shopName)
                                                Toast.makeText(
                                                    this,
                                                    "Item Uploaded Successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }.addOnFailureListener {
                                                Toast.makeText(
                                                    this,
                                                    "Item Upload Failed",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    }
                                }.addOnFailureListener {
                                    Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Please Select an Image", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Shop name not found", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to retrieve shop name", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Shop ID not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to retrieve shop ID", Toast.LENGTH_SHORT).show()
        }
    }

}