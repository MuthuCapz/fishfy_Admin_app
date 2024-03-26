package com.example.admin.ui.activities


import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.admin.R
import com.example.admin.databinding.ActivityAddItem2Binding
import com.example.admin.databinding.ActivityAddItem3Binding
import com.example.admin.models.AllMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlin.random.Random

class AddItem3Activity : AppCompatActivity() {

    // Food item Details
    private lateinit var foodName: String
    private lateinit var foodPrice: String
    private lateinit var foodDescription: String
    private lateinit var Quantity: String
    private lateinit var stock: String
    private lateinit var category: String
    private var foodImageUri: Uri? = null

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val binding: ActivityAddItem3Binding by lazy {
        ActivityAddItem3Binding.inflate(layoutInflater)
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
            foodDescription = binding.editTextDescription.text.toString().trim()
            Quantity = binding.editTextIngredients.text.toString().trim()
            category = binding.categoryadd2.text.toString().trim()
            stock = binding.stock.text.toString().trim()


            if (!(foodName.isBlank() || foodPrice.isBlank() || foodDescription.isBlank() || Quantity.isBlank() || category.isBlank())) {
                uploadData()
                Toast.makeText(this, "Item Add Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Fill All the Details", Toast.LENGTH_SHORT).show()
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

    private fun uploadData() {

        // get a reference to the "Menu" node in the database
        val menuRef = database.getReference("menu2")
        // Generate a unique key for the new menu item
        val newSKUPrefix = "SKU-"
        val newSKU = newSKUPrefix + (100000 + Random.nextInt(900000))

        if (foodImageUri != null) {

            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("menu_images/${newSKU}.jpg")
            val uploadTask = imageRef.putFile(foodImageUri!!)

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // Create  a new menu item
                    val newItem = AllMenu(
                        newSKU,
                        foodName,
                        foodPrice,
                        foodDescription,
                        Quantity,

                        downloadUrl.toString(),
                        category,
                        stock
                    )
                    newSKU?.let { key ->
                        menuRef.child(key).setValue(newItem).addOnSuccessListener {
                            Toast.makeText(this, "item Uploaded Successfully", Toast.LENGTH_SHORT)
                                .show()
                        }.addOnFailureListener {
                            Toast.makeText(this, "item Uploaded Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Image Uploaded Failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please Select an Image", Toast.LENGTH_SHORT).show()
        }
    }
}