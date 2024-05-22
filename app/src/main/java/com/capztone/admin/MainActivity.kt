package com.capztone.admin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.capztone.admin.databinding.ActivityMainBinding
import com.capztone.admin.ui.activities.AddItem2Activity
import com.capztone.admin.ui.activities.AddItem3Activity
import com.capztone.admin.ui.activities.AddItemActivity
import com.capztone.admin.ui.activities.AllItemsActivity
import com.capztone.admin.ui.activities.DiscountActivity
import com.capztone.admin.ui.activities.Inventory
import com.capztone.admin.ui.activities.LoginActivity
import com.capztone.admin.ui.activities.OrderDetailsActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var menuRef: DatabaseReference
    private lateinit var menu1Ref: DatabaseReference
    private lateinit var menu2Ref: DatabaseReference
    private lateinit var discountRef:DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var adminRef: DatabaseReference

    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Initialize Firebase database references

        val userId = auth.currentUser?.uid

        if (userId != null) {
            adminRef = FirebaseDatabase.getInstance().getReference("Admins").child(userId)
            fetchShopNameAndCounts()
        } else {
            // Handle the case when userId is null
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show()
        }
        // Setup onClickListeners for buttons
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        binding.addMenuButton.setOnClickListener {
            startActivity(Intent(this, AddItemActivity::class.java))
        }

        binding.allmenuitem1.setOnClickListener {
            startActivity(Intent(this, AddItem2Activity::class.java))
        }

        binding.allmenuitem2.setOnClickListener {
            startActivity(Intent(this, AddItem3Activity::class.java))
        }

        binding.allItemMenu.setOnClickListener {
            startActivity(Intent(this, AllItemsActivity::class.java))
        }

        binding.cardView77.setOnClickListener {
            startActivity(Intent(this, Inventory::class.java))
        }
        binding.cardView78.setOnClickListener {
            startActivity(Intent(this, OrderDetailsActivity::class.java))
        }
        binding.discount.setOnClickListener {
            startActivity(Intent(this, DiscountActivity::class.java))
        }
        binding.logout.setOnClickListener {
            logout()
        }

        // Fetch counts from Firebase and update TextViews
        fetchShopNameAndCounts()
    }



    private fun logout() {
        googleSignInClient.signOut().addOnCompleteListener(this, OnCompleteListener {
            // After signing out, move to login activity
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        })

    }
    private fun fetchShopNameAndCounts() {
        adminRef.child("shopName").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val shopName = snapshot.getValue(String::class.java)
                if (shopName != null) {
                    fetchCounts(shopName)
                } else {
                    Toast.makeText(this@MainActivity, "Shop name not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to fetch shop name: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchCounts(shopName: String) {
        val menuRef = FirebaseDatabase.getInstance().getReference(shopName).child("menu")
        val menu1Ref = FirebaseDatabase.getInstance().getReference(shopName).child("menu1")
        val menu2Ref = FirebaseDatabase.getInstance().getReference(shopName).child("menu2")
        val discountRef = FirebaseDatabase.getInstance().getReference(shopName).child("discount")

        menuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.count.text = "Total: $count"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to fetch count: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        menu1Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.count1.text = "Total: $count"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to fetch count: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        menu2Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.count2.text = "Total: $count"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to fetch count: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        discountRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.count6.text = "Total: $count"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to fetch count: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
