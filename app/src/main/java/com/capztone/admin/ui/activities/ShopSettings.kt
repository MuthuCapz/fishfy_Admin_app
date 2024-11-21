package com.capztone.admin.ui.activities

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivityShopSettingsBinding

import com.capztone.admin.models.Shop
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ShopSettings : AppCompatActivity() {

    private lateinit var binding: ActivityShopSettingsBinding
    private lateinit var database: DatabaseReference
    private var shopList: MutableList<String> = mutableListOf()
    private var startTime: Calendar = Calendar.getInstance()
    private var endTime: Calendar = Calendar.getInstance()
    private val timeSlots = mutableListOf<String>()
    private lateinit var deliveryDetailsRef: DatabaseReference
    private lateinit var shopNamesRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShopSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Realtime Database
        database = FirebaseDatabase.getInstance().reference

        // Initialize Firebase references
        deliveryDetailsRef = FirebaseDatabase.getInstance().getReference("Delivery Details")
        shopNamesRef = FirebaseDatabase.getInstance().getReference("ShopNames")

        fetchShopIdAndShopName()

        val shopName = intent.getStringExtra("shopName")
        if (!shopName.isNullOrEmpty()) {
            binding.selectShop.text = shopName
        } else {
            binding.selectShop.text = "Unknown Shop" // Default text if no shop name is passed
        }
        // Finish Activity
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.userDistance.setOnClickListener {
            showDistanceDropdownMenu(it)
        }
        binding.driverDistance.setOnClickListener {
            showDriverDistanceDropdownMenu(it)
        }
        binding.continueButton.setOnClickListener {
            val newSlotTime = binding.slotValue.text.toString()
            val slotTimesList = newSlotTime.lines().map { it.trim() }.filter { it.isNotEmpty() }

            // Check if the user has selected exactly 3 time slots
            if (slotTimesList.size < 3) {
                val remainingSlots = 3 - slotTimesList.size
                Toast.makeText(
                    this,
                    "Please select $remainingSlots more time slot(s) to proceed.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                saveDataToFirebase()
            }
        }


        binding.time.setOnClickListener {
            showCustomTimePickerDialog()
        }
        binding.slotValue.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.scrollView.postDelayed({
                    binding.scrollView.smoothScrollTo(0, binding.slotValue.bottom)
                }, 300)
            }
        }
// Handle Free Delivery selection
        binding.rbFreeDelivery.setOnClickListener {
            if (binding.rbFreeDelivery.isChecked) {

                // Change color to orange when selected
                binding.rbFreeDelivery.setButtonTintList(ContextCompat.getColorStateList(this, R.color.navy))
            } else {
                // Reset to default color when unselected
                binding.rbFreeDelivery.setButtonTintList(ContextCompat.getColorStateList(this, R.color.black)) // Default color (black or your default)
            }
        }
        // Assuming you have a Firebase reference set up
        val database = FirebaseDatabase.getInstance()
        val deliveryDetailsRef = database.getReference("Delivery Details")
        val shopNamesRef = database.getReference("ShopNames")

// Step 1: Query to get the Shop ID
        deliveryDetailsRef.child("Shop Id").get().addOnSuccessListener { shopIdSnapshot ->
            // Check if the shop ID snapshot exists
            if (shopIdSnapshot.exists()) {
                // Get the shop ID value
                val shopId = shopIdSnapshot.getValue(String::class.java)

                // Proceed if a shop ID is found
                if (shopId != null) {
                    // Step 2: Fetch the details for the found shop ID
                    deliveryDetailsRef.child(shopId).get().addOnSuccessListener { shopDetailsSnapshot ->
                        if (shopDetailsSnapshot.exists()) {
                            // Retrieve the values
                            val baseFare = shopDetailsSnapshot.child("Base Fare").getValue(String::class.java) ?: "0"
                            val userDistance = shopDetailsSnapshot.child("User Distance").getValue(String::class.java) ?: "0"
                            val driverDistance = shopDetailsSnapshot.child("Driver Distance").getValue(String::class.java) ?: "0"
                            val gstValue = shopDetailsSnapshot.child("GST").getValue(String::class.java) ?: "0"
                            val speedDeliveryValue = shopDetailsSnapshot.child("Speed Delivery Charge").getValue(String::class.java) ?: "0"
                            val peakValue = shopDetailsSnapshot.child("PeakValue").getValue(String::class.java) ?: "0"
                            val perKmChargeValue = shopDetailsSnapshot.child("PerKm Charge").getValue(String::class.java) ?: "0"
                            val serviceChargeValue = shopDetailsSnapshot.child("Service Charge").getValue(String::class.java) ?: "0"

                            // Now that we have the shop ID, fetch the shop name from ShopNames
                            shopNamesRef.child(shopId).child("shopName").get().addOnSuccessListener { shopNameSnapshot ->
                                if (shopNameSnapshot.exists()) {
                                    // Get the shop name value
                                    val shopName = shopNameSnapshot.getValue(String::class.java) ?: "Unknown Shop"
                                    // Set the shop name to the selectShop TextView
                                } else {
                                    // If shop name doesn't exist, use default text
                                }

                                // Set the other values to your TextViews
                                binding.userDistance.setText(userDistance)
                                binding.driverDistance.setText(driverDistance)
                                binding.gstValue.setText(gstValue)
                                binding.baseFarevalue.setText(baseFare)
                                binding.speedDeliveryValue.setText(speedDeliveryValue)
                                binding.peakValue.setText(peakValue)
                                binding.perkmChargeValue.setText(perKmChargeValue)
                                binding.serviceChargeValue.setText(serviceChargeValue)
                            }.addOnFailureListener { exception ->
                                // Handle potential errors for fetching shop name
                                Log.e("FirebaseError", "Error getting shop name", exception)
                            }
                        } else {
                            // Set default values if the shop details do not exist
                            setDefaultValues()
                        }
                    }.addOnFailureListener { exception ->
                        // Handle potential errors for fetching shop details
                        Log.e("FirebaseError", "Error getting shop details", exception)
                        setDefaultValues()
                    }
                } else {
                    // No shop ID found, set default values
                    setDefaultValues()
                }
            } else {
                // No shop ID found, set default values
                setDefaultValues()
            }
        }.addOnFailureListener { exception ->
            // Handle potential errors for fetching shop ID
            Log.e("FirebaseError", "Error getting delivery details", exception)
            setDefaultValues()
        }
        fetchShopNamesFromFirebase()
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
                        Toast.makeText(this@ShopSettings, "Shop ID not found in Delivery details", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ShopSettings, "No Shop ID found in Delivery details", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ShopSettings, "Failed to fetch Shop ID: ${error.message}", Toast.LENGTH_SHORT).show()
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

                    } else {
                        Toast.makeText(this@ShopSettings, "Shop name not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ShopSettings, "Shop ID not found in ShopNames", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ShopSettings, "Failed to fetch shop name: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    // Function to set default values
    private fun setDefaultValues() {

        binding.userDistance.setText("15")
        binding.driverDistance.setText("20")
        binding.gstValue.setText("0")
        binding.baseFarevalue.setText("20")
        binding.speedDeliveryValue.setText("0")
        binding.peakValue.setText("0")
        binding.perkmChargeValue.setText("5")
        binding.serviceChargeValue.setText("5")
    }
    private fun showCustomTimePickerDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_time_picker, null)
        val startTimePicker = dialogView.findViewById<TimePicker>(R.id.startTimePicker)
        val endTimePicker = dialogView.findViewById<TimePicker>(R.id.endTimePicker)
        val confirmButton = dialogView.findViewById<Button>(R.id.confirmButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        // Initialize TimePickers to 12-hour format
        startTimePicker.setIs24HourView(false)
        endTimePicker.setIs24HourView(false)

        // Set up dialog
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        confirmButton.setOnClickListener {
            if (timeSlots.size >= 3) {
                Toast.makeText(this, "You can select up to 3 time slots only.", Toast.LENGTH_SHORT).show()
            } else {
                // Get start time
                val startHour = startTimePicker.hour
                val startMinute = startTimePicker.minute
                startTime.set(Calendar.HOUR_OF_DAY, startHour)
                startTime.set(Calendar.MINUTE, startMinute)

                // Get end time
                val endHour = endTimePicker.hour
                val endMinute = endTimePicker.minute
                endTime.set(Calendar.HOUR_OF_DAY, endHour)
                endTime.set(Calendar.MINUTE, endMinute)

                // Format the selected time slot and update UI
                val formattedTime = formatSlotTime(startTime, endTime)
                // Check if the formatted time is already in the list
                if (!timeSlots.contains(formattedTime)) {
                    timeSlots.add(formattedTime)
                    binding.slotValue.setText(timeSlots.joinToString("\n"))
                } else {
                    Toast.makeText(
                        this,
                        "This time slot has already been selected.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
                alertDialog.dismiss()

        }

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        // Show the dialog
        alertDialog.show()
    }
    private fun formatSlotTime(start: Calendar, end: Calendar): String {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val startTimeFormatted = timeFormat.format(start.time)
        val endTimeFormatted = timeFormat.format(end.time)
        return "$startTimeFormatted - $endTimeFormatted"
    }

    private fun saveDataToFirebase() {
        val selectedShop = binding.selectShop.text.toString()
        val userDistance = binding.userDistance.text.toString()
        val driverDistance = binding.driverDistance.text.toString()
        val baseFare = binding.baseFarevalue.text.toString()
        val gst = binding.gstValue.text.toString()
        val peakCharge = binding.peakValue.text.toString()
        val speed = binding.speedDeliveryValue.text.toString()
        val perkmCharge = binding.perkmChargeValue.text.toString()
        val serviceCharge = binding.serviceChargeValue.text.toString()
        val newSlotTime = binding.slotValue.text.toString() // New input for slot time

        // Check if all required fields are filled
        if (selectedShop.isEmpty() || userDistance.isEmpty() || driverDistance.isEmpty() || baseFare.isEmpty() ||
            gst.isEmpty() || peakCharge.isEmpty() || speed.isEmpty() || perkmCharge.isEmpty() || serviceCharge.isEmpty()) {
            Toast.makeText(this, "Please fill in all the required fields.", Toast.LENGTH_SHORT).show()
            return
        }

        // Get delivery type and amount only if Free Delivery is selected
        val deliveryType = if (binding.rbFreeDelivery.isChecked) "Free Delivery" else null
        val deliveryAmount = if (binding.rbFreeDelivery.isChecked) binding.etDeliveryAmount.text.toString() else null

        if (deliveryType == "Free Delivery" && deliveryAmount.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter the delivery amount for Free Delivery.", Toast.LENGTH_SHORT).show()
            return
        }

        // Format the delivery amount if available
        val formattedAmount = if (!deliveryAmount.isNullOrEmpty()) "UPTO ₹$deliveryAmount" else null
        val slotTimesList = newSlotTime.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)
        val username = account?.displayName ?: "Unknown User"
        val sdfDate = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
        val currentDate = sdfDate.format(Date())

        if (userId != null) {
            val deliveryRef = database.child("Delivery Details")
            val shopNamesRef = database.child("ShopNames")
            shopNamesRef.get().addOnSuccessListener { shopSnapshot ->
                var shopId: String? = null
                for (shop in shopSnapshot.children) {
                    val shopName = shop.child("shopName").getValue(String::class.java)
                    if (shopName != null && shopName.equals(selectedShop, ignoreCase = true)) {
                        shopId = shop.key
                        break
                    }
                }

                if (shopId != null) {
                    val selectedShopRef = deliveryRef.child(shopId)
                    deliveryRef.child("Shop Id").setValue(shopId)
                    deliveryRef.child("User Distance").setValue(userDistance)
                    deliveryRef.child("Driver Distance").setValue(driverDistance)

                    selectedShopRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            selectedShopRef.apply {
                                child("User Distance").setValue(userDistance)
                                child("Driver Distance").setValue(driverDistance)
                                child("Base Fare").setValue(baseFare)
                                child("GST").setValue(gst)
                                child("Peak Hour Charge").setValue(peakCharge)
                                child("Speed Delivery Charge").setValue(speed)
                                child("Perkm Charge").setValue(perkmCharge)
                                child("Service Charge").setValue(serviceCharge)
                                child("UpdatedBy").setValue(username)
                                child("UpdatedDate").setValue(currentDate)
                                // Add delivery type and amount only if Free Delivery
                                if (deliveryType == "Free Delivery") {
                                    child("Delivery Type").setValue(deliveryType)
                                    child("Delivery Amount").setValue(formattedAmount)
                                } else {
                                    // Remove fields for Paid Delivery
                                    child("Delivery Type").removeValue()
                                    child("Delivery Amount").removeValue()
                                }
                            }
                        } else {
                            selectedShopRef.apply {
                                child("User Distance").setValue(userDistance)
                                child("Driver Distance").setValue(driverDistance)
                                child("Base Fare").setValue(baseFare)
                                child("GST").setValue(gst)
                                child("Peak Hour Charge").setValue(peakCharge)
                                child("Speed Delivery Charge").setValue(speed)
                                child("Perkm Charge").setValue(perkmCharge)
                                child("Service Charge").setValue(serviceCharge)
                                child("CreatedBy").setValue(username)
                                child("CreatedDate").setValue(currentDate)
                                if (deliveryType == "Free Delivery") {
                                    child("Delivery Type").setValue(deliveryType)
                                    child("Delivery Amount").setValue(deliveryAmount)
                                } else {
                                    child("Delivery Type").removeValue()
                                    child("Delivery Amount").removeValue()
                                }
                            }
                        }

                        val slotTimesRef = selectedShopRef.child("Slot Timings")
                        slotTimesRef.setValue(slotTimesList)

                        slotTimesRef.get().addOnSuccessListener {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(this, "Failed to save data. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to retrieve shop details. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Selected shop not found. Please check the shop name.", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to retrieve shop names. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val shopMap = mutableMapOf<String, String>()  // Map to store shopId and shopName pairs

    private fun fetchShopNamesFromFirebase() {
        database.child("ShopNames").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                shopMap.clear()  // Clear previous data
                dataSnapshot.children.forEach {
                    val shopId = it.key ?: return@forEach  // Get the shop ID
                    val shopName = it.child("shopName").getValue(String::class.java) ?: ""
                    shopMap[shopId] = shopName  // Store in the map
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle possible errors.
            }
        })
    }

    private fun showDistanceDropdownMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menu.add("5")
        popupMenu.menu.add("10")
        popupMenu.menu.add("15")
        popupMenu.menu.add("20")

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            val selectedDistance = menuItem.title.toString()
            binding.userDistance.setText(selectedDistance)
            true
        }

        popupMenu.show()
    }

    private fun showDriverDistanceDropdownMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menu.add("5")
        popupMenu.menu.add("10")
        popupMenu.menu.add("15")
        popupMenu.menu.add("20")

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            val selectedDistance = menuItem.title.toString()
            binding.driverDistance.setText(selectedDistance)
            true
        }

        popupMenu.show()
    }

}

data class Shop1(
    val shopName: String = "",
    val password: String = "",
    val CreatedDate: String = "", // Field for storing the date
    val CreateddBy: String = "" // Field for storing Google login username
)