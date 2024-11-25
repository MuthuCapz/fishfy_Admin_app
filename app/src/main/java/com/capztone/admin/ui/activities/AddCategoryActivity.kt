package com.capztone.admin.ui.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.capztone.admin.databinding.ActivityAddCategoryBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCategoryBinding
    private var imageUri: Uri? = null
    private var shopId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back Button
        binding.backButton.setOnClickListener { finish() }

        // Upload Image Click
        binding.categoryImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 100)
        }

        fetchShopId()
        // Save Button Click
        binding.saveButton.setOnClickListener {
            var categoryName = binding.categoryNameEditText.text.toString().trim()

            // Check if the category name is empty
            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Normalize category name for discount variations and minor misspellings
            val normalizedCategoryName = categoryName.lowercase(Locale.getDefault()).trim()

// Check if the category name matches variations of "discount" or common misspellings
            if (normalizedCategoryName in listOf("discount", "discounts", "discunt", "disount", "disscount","Discount","Discounts")) {
                categoryName = "discount"  // Store as "discount"
            }


            // Check if the image URI is null
            if (imageUri == null) {
                Toast.makeText(this, "Please add an image for the category", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Proceed to upload the image and save the category
            uploadImageToFirebase(categoryName, shopId!!)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            binding.categoryImageView.setImageURI(imageUri)
        }
    }
    private fun fetchShopId() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val shopIdRef = userId?.let {
            FirebaseDatabase.getInstance().reference
                .child("Admins").child(it)
                .child("Shop Id")
        } // Changed to ShopId path

        if (shopIdRef != null) {
            shopIdRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    shopId = dataSnapshot.getValue(String::class.java)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@AddCategoryActivity, "Failed to retrieve shop ID", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun uploadImageToFirebase(categoryName: String, shopId: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("Categories/${UUID.randomUUID()}")
        imageUri?.let { uri ->
            storageRef.putFile(uri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveCategoryToFirebase(categoryName, downloadUri.toString(), shopId)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun saveCategoryToFirebase(categoryName: String, imageUrl: String?, shopId: String) {
        val categoryRef = FirebaseDatabase.getInstance().reference
            .child("Categories")
            .child(shopId) // Changed to use Shop ID
            .child(categoryName)
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val CreatedBy = account?.displayName ?: "Unknown User"
        // Get current date and time
        val CurrentDate = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())


        val categoryData = mapOf(
            "image" to imageUrl,
            "CreatedDate" to CurrentDate,
            "CreatedBy" to CreatedBy // Store Google login username
        )
        categoryRef.setValue(categoryData).addOnCompleteListener {
            if (it.isSuccessful) {
                Toast.makeText(this, "Category saved under Shop ID: $shopId", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save category", Toast.LENGTH_SHORT).show()
            }
        }
    }
}