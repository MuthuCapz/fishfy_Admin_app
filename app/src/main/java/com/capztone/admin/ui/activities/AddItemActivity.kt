package com.capztone.admin.ui.activities


import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.capztone.admin.databinding.ActivityAddItem2Binding
import com.capztone.admin.databinding.ActivityAddItemBinding
import com.capztone.admin.models.AllMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlin.random.Random

class AddItemActivity : AppCompatActivity() {

    // Food item Details
    private lateinit var foodName :String
    private lateinit var foodPrice :String
    private lateinit var foodDescription :String
    private lateinit var Quantity :String
    private lateinit var stock :String
    private lateinit var category:String
    private var foodImageUri : Uri? = null

    // Firebase
    private lateinit var auth :FirebaseAuth
    private lateinit var database:FirebaseDatabase

    private val binding: ActivityAddItemBinding by lazy {
        ActivityAddItemBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        //Initialize Firebase
        auth = FirebaseAuth.getInstance()
        // Initialize Firebase database Instance
        database = FirebaseDatabase.getInstance()

        // Finish Activity
        binding.backButton.setOnClickListener {
            finish()
        }
        ///
        binding.addItemButton.setOnClickListener {
            // GET Data form Filed EditText
            foodName = binding.editTextFoodName.text.toString().trim()
            foodPrice = binding.edittextFoodPrice.text.toString().trim()
            foodDescription= binding.editTextDescription.text.toString().trim()
            Quantity = binding.editTextIngredients.text.toString().trim()
            category = binding.category.text.toString().trim()
            stock = binding.stock.text.toString().trim()

            val userId = auth.currentUser?.uid

            if (!(foodName.isBlank() || foodPrice.isBlank() || foodDescription.isBlank()|| Quantity.isBlank()||category.isBlank())){
                uploadData(userId)
                Toast.makeText(this,"Item Add Successfully",Toast.LENGTH_SHORT).show()
                finish()
            }else {
                Toast.makeText(this,"Fill All the Details",Toast.LENGTH_SHORT).show()
            }
        }

        // Image
        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

    }



    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.selectedImageView.setImageURI(uri)
            foodImageUri = uri
        }
    }

    private fun uploadData(userId: String?) {
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Get a reference to the "Admins" node to retrieve the shop name
        val adminRef = database.getReference("Admins").child(userId)

        adminRef.child("shopName").get().addOnSuccessListener { dataSnapshot ->
            val shopName = dataSnapshot.getValue(String::class.java)
            if (shopName != null) {
                // If the shop name exists, proceed with uploading the data

                // Get a reference to the specific shop's menu in the database
                val menuRef = database.getReference(shopName).child("menu")
                // Generate a unique key for the new menu item
                val newSKUPrefix = "SKU-"
                val newSKU = newSKUPrefix + (100000 + Random.nextInt(900000))

                if (foodImageUri != null) {
                    val storageRef = FirebaseStorage.getInstance().reference
                    val imageRef = storageRef.child("menu_images/${newSKU}.jpg")
                    val uploadTask = imageRef.putFile(foodImageUri!!)

                    uploadTask.addOnSuccessListener {
                        imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            // Create a new menu item
                            val newItem = AllMenu(
                                newSKU,
                                foodName,
                                foodPrice,
                                foodDescription,
                                Quantity,
                                downloadUrl.toString(),
                                category,
                                stock,
                                userId
                            )
                            // Add the new item to the menu
                            menuRef.child(newSKU).setValue(newItem).addOnSuccessListener {
                                Toast.makeText(this, "Item Uploaded Successfully", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Item Upload Failed", Toast.LENGTH_SHORT).show()
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
    }

}