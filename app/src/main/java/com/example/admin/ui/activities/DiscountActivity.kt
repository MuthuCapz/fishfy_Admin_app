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
import com.example.admin.databinding.ActivityDiscountBinding
import com.example.admin.models.AllMenu
import com.example.admin.models.DiscountItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlin.random.Random

class DiscountActivity : AppCompatActivity() {

    // Food item Details
    private lateinit var foodNames:String
    private lateinit var foodPrices :String
    private lateinit var foodDescriptions :String
    private lateinit var Quantitys :String
    private lateinit var stocks :String
    private lateinit var discounts:String
    private lateinit var categorys:String
    private var foodImageUri : Uri? = null

    // Firebase
    private lateinit var auth :FirebaseAuth
    private lateinit var database:FirebaseDatabase

    private val binding: ActivityDiscountBinding by lazy {
        ActivityDiscountBinding.inflate(layoutInflater)
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
            foodNames = binding.editTextFoodName.text.toString().trim()
            foodPrices = binding.edittextFoodPrice.text.toString().trim()
            foodDescriptions= binding.editTextDescription.text.toString().trim()
            Quantitys = binding.editTextIngredients.text.toString().trim()
            categorys = binding.categoryadd1.text.toString().trim()
            stocks = binding.stock.text.toString().trim()
            discounts = binding.edittextDiscount.text.toString().trim()


            if (!(foodNames.isBlank() || foodPrices.isBlank() || foodDescriptions.isBlank()|| Quantitys.isBlank()||categorys.isBlank()||discounts.isBlank())){
                uploadData()
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

    private fun uploadData() {

        // get a reference to the "Menu" node in the database
        val menuRef = database.getReference("discount")
        // Generate a unique key for the new menu item
        val newSKUPrefix = "SKU-"
        val newSKU = newSKUPrefix + (100000 + Random.nextInt(900000))

        if (foodImageUri != null){

            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("menu_images/${newSKU}.jpg")
            val uploadTask = imageRef.putFile(foodImageUri!!)

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener {
                        downloadUrl->
                    // Create  a new menu item
                    val newItem = DiscountItem(
                        newSKU,
                        foodNames,
                        foodPrices,
                        foodDescriptions,
                        Quantitys,
                        downloadUrl.toString(),
                        categorys,
                        stocks,
                        discounts

                    )
                    newSKU?.let {
                            key->
                        menuRef.child(key).setValue(newItem).addOnSuccessListener {
                            Toast.makeText(this,"item Uploaded Successfully",Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            Toast.makeText(this,"item Uploaded Failed",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this,"Image Uploaded Failed",Toast.LENGTH_SHORT).show()
            }
        }else {
            Toast.makeText(this,"Please Select an Image",Toast.LENGTH_SHORT).show()
        }
    }


}