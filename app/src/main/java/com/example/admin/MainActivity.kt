package com.example.admin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.admin.databinding.ActivityMainBinding
import com.example.admin.ui.activities.AddItem2Activity
import com.example.admin.ui.activities.AddItem3Activity
import com.example.admin.ui.activities.AddItemActivity
import com.example.admin.ui.activities.AllItemsActivity
import com.example.admin.ui.activities.DiscountActivity
import com.example.admin.ui.activities.Inventory
import com.example.admin.ui.activities.LoginActivity
import com.example.admin.ui.activities.OrderDetailsActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var menuRef: DatabaseReference
    private lateinit var menu1Ref: DatabaseReference
    private lateinit var menu2Ref: DatabaseReference
    private lateinit var discountRef:DatabaseReference
    private lateinit var googleApiClient: GoogleApiClient
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase database references
        menuRef = FirebaseDatabase.getInstance().getReference("menu")
        menu1Ref = FirebaseDatabase.getInstance().getReference("menu1")
        menu2Ref = FirebaseDatabase.getInstance().getReference("menu2")
        discountRef = FirebaseDatabase.getInstance().getReference("discount")

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
        fetchCounts()
    }



    private fun logout() {
        googleSignInClient.signOut().addOnCompleteListener(this, OnCompleteListener {
            // After signing out, move to login activity
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        })

    }
    private fun fetchCounts() {
        menuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.count.text = "Total: $count"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Toast.makeText(this@MainActivity, "Failed to fetch count: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        menu1Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.count1.text = "Total: $count"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Toast.makeText(this@MainActivity, "Failed to fetch count: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        menu2Ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.count2.text = "Total: $count"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Toast.makeText(this@MainActivity, "Failed to fetch count: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        discountRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                binding.count6.text = "Total: $count"
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
                Toast.makeText(this@MainActivity, "Failed to fetch count: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
