package com.example.admin.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.PopupMenu
import com.opencsv.CSVReaderBuilder
import java.io.InputStreamReader
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.admin.adapters.InventoryAdapter
import com.example.admin.databinding.ActivityInventoryBinding
import com.example.admin.models.RetrieveItem
import com.example.admin.R
import com.example.admin.models.DiscountItem
import com.google.firebase.database.*
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException


class Inventory : AppCompatActivity() {

    private lateinit var binding: ActivityInventoryBinding
    private lateinit var adapter: InventoryAdapter
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    importDataFromCSV(uri)
                } ?: run {
                    // Handle the case when user cancels file selection
                    Toast.makeText(
                        applicationContext,
                        "File selection cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    private val importLaunchers =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    importDataFromCSVs(uri)
                } ?: run {
                    // Handle the case when user cancels file selection
                    Toast.makeText(
                        applicationContext,
                        "File selection cancelled",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private fun importDataFromCSVs(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = CSVReaderBuilder(InputStreamReader(inputStream, "UTF-8")).build()

            var record: Array<String>?

            // Skip header line if exists
            reader.readNext()

            while (reader.readNext().also { record = it } != null) {
                if (record?.size == 9) { // Assuming there are 8 fields in each record
                    val foodNames = record!![0].trim()
                    val foodPrices = record!![1].trim()
                    val key = record!![2].trim() ?: continue
                    val imageUrl = record!![3].trim()
                    val categorys = record!![4].trim()
                    val stocks = record!![5].trim()
                    val quantitys = record!![6].trim()
                    val foodDescriptions = record!![7].trim()
                    val discounts = record!![8].trim()
                    // Assuming the last field indicates if the product is discounted

                    // Determine the Firebase path based on category
                    val databaseReference = when (categorys.toLowerCase()) {

                        "discount" -> FirebaseDatabase.getInstance().reference.child("discount")
                        else -> null // Handle other categories if needed
                    }

                    // If the category is recognized, create a new item object and upload to Firebase
                    databaseReference?.let {
                        val newItem = DiscountItem(
                            foodNames,
                            foodPrices,
                            key,
                            imageUrl,
                            categorys,
                            stocks,
                            quantitys,
                            foodDescriptions,
                            discounts
                        )
                        val newItemRef = it.child(foodPrices)
                        newItemRef.setValue(newItem)
                    }
                }
            }

            Toast.makeText(
                applicationContext,
                "Inventory imported successfully",
                Toast.LENGTH_SHORT
            ).show()
        } ?: run {
            Toast.makeText(
                applicationContext,
                "Failed to open CSV file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.save.setOnClickListener {
            saveDataToFirebase()
        }

        binding.Addmore.setOnClickListener {
            showPopupMenu(it)
        }

        // Initialize RecyclerView and adapter
        adapter = InventoryAdapter(this)
        binding.recyclerView1.layoutManager = LinearLayoutManager(this)
        binding.recyclerView1.adapter = adapter

        // Fetch data from Firebase and populate RecyclerView
        adapter.fetchData()
    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.inflate(R.menu.addmore)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_import -> {
                    // Pass the anchor view to showImportOptions
                    showImportOptions(view)
                    true
                }
                R.id.menu_export -> {
                    exportDataToCSV()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }


    private fun showImportOptions(anchorView: View) {
        val importOptions = PopupMenu(this, anchorView) // Pass the anchor view here
        importOptions.menu.add("Products").setOnMenuItemClickListener {
            chooseFileForImport()
            true
        }
        importOptions.menu.add("Discounts").setOnMenuItemClickListener {
            chooseFileForImports()
            true
        }
        importOptions.show()
    }




    private fun chooseFileForImport() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/csv" // Set the MIME type to filter only CSV files
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        importLauncher.launch(Intent.createChooser(intent, "Choose CSV file"))
    }
    private fun chooseFileForImports() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "text/csv" // Set the MIME type to filter only CSV files
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        importLaunchers.launch(Intent.createChooser(intent, "Choose CSV file"))
    }



    private fun importDataFromCSV(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = CSVReaderBuilder(InputStreamReader(inputStream, "UTF-8")).build()

            var record: Array<String>?

            // Skip header line if exists
            reader.readNext()

            while (reader.readNext().also { record = it } != null) {
                if (record?.size == 9) { // Assuming there are 8 fields in each record
                    val foodName = record!![0].trim()
                    val foodPrice = record!![1].trim()
                    val key = record!![2].trim() ?: continue
                    val imageUrl = record!![3].trim()
                    val category = record!![4].trim()
                    val stock = record!![5].trim()
                    val quantity = record!![6].trim()
                    val foodDescription = record!![7].trim()
                    val discounts = record!![8].trim()
                    // Assuming the last field indicates if the product is discounted

                    // Determine the Firebase path based on category
                    val databaseReference = when (category.toLowerCase()) {
                        "deal of the day" -> FirebaseDatabase.getInstance().reference.child("menu")
                        "dry fish" -> FirebaseDatabase.getInstance().reference.child("menu1")
                        "pickles" -> FirebaseDatabase.getInstance().reference.child("menu2")
                        else -> null // Handle other categories if needed
                    }

                    // If the category is recognized, create a new item object and upload to Firebase
                    databaseReference?.let {
                        val newItem = RetrieveItem(
                            foodName,
                            foodPrice,
                            key,
                            imageUrl,
                            category,
                            stock,
                            quantity,
                            foodDescription,
                            discounts
                        )
                        val newItemRef = it.child(foodPrice)
                        newItemRef.setValue(newItem)
                    }
                }
            }

            Toast.makeText(
                applicationContext,
                "Inventory imported successfully",
                Toast.LENGTH_SHORT
            ).show()
        } ?: run {
            Toast.makeText(
                applicationContext,
                "Failed to open CSV file",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun saveDataToFirebase() {
        // Get the updated items from the adapter
        val items = adapter.getItems()

        // Save the updated items to Firebase
        for (item in items) {
            val key = item.key
            key?.let {
                database.child("Inventory").child(it).setValue(item)
                    .addOnSuccessListener {
                        // Data saved successfully
                        Toast.makeText(this, "Updated Inventory successfully", Toast.LENGTH_SHORT)
                            .show()
                    }
                    .addOnFailureListener {
                        // Failed to save data
                        Toast.makeText(this, "Failed to update Inventory", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
        }
    }

    private fun exportDataToCSV() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("Inventory")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    try {
                        val csvFile = File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "imp.csv"
                        )
                        val csvWriter = FileWriter(csvFile)
                        csvWriter.append("Food Name,SKU ID,Food Price,Image Url,Category,Stock,Quantity\n")

                        for (itemSnapshot in snapshot.children) {
                            val item = itemSnapshot.getValue(RetrieveItem::class.java)
                            item?.let {
                                val line =
                                    "${it.foodName},${it.key},${it.foodPrice},${it.foodImage},${it.category},${it.stock},${it.quantity}\n"
                                csvWriter.append(line)
                            }
                        }
                        csvWriter.flush()
                        csvWriter.close()
                        Toast.makeText(
                            applicationContext,
                            "Inventory exported successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(
                            applicationContext,
                            "Error exporting inventory: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        applicationContext,
                        "No data available to export",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    applicationContext,
                    "Failed to export inventory: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
