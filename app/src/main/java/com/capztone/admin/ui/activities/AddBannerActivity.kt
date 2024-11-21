package com.capztone.admin.ui.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.databinding.ActivityAddBannerBinding
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
class AddBannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddBannerBinding

    private var selectedImageUri: Uri? = null

    private val getContent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                selectedImageUri = data?.data
                binding.selectedImageView.setImageURI(selectedImageUri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddBannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.selectImage.setOnClickListener {
            openGallery()
        }

        // Finish Activity
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.addbutton.setOnClickListener {
            selectedImageUri?.let { uri ->
                uploadImageToFirebase(uri)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getContent.launch(intent)
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("Banners")
        val imageName = UUID.randomUUID().toString()
        val imageRef = storageRef.child("$imageName.jpg")

        val uploadTask = imageRef.putFile(imageUri)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            // Image uploaded successfully
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                // Now you can use downloadUri to store the image location in your database
                val imageUrl = downloadUri.toString()
                // Here you can save the imageUrl to your database under "Banners" path
                // Example: Save imageUrl to Firebase Realtime Database or Firestore
                Toast.makeText(this, "Banner Added Successfully", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                // Handle any errors during getting download URL
            }
        }.addOnFailureListener { exception ->
            // Handle unsuccessful uploads
        }
    }
}
