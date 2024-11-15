package com.capztone.admin.ui.activities

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivityMainAddNewShopBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.OnCompleteListener


class MainAddNewShopActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainAddNewShopBinding
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ViewBinding
        binding = ActivityMainAddNewShopBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                window.statusBarColor = getColor(R.color.navy)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                window.statusBarColor = Color.TRANSPARENT
            }
        }

        // Setup onClickListeners for buttons
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        // Set window insets padding
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        disableSearchViewInput(binding.searchView1)
        // Set up delete account button listener
        binding.deleteaccount.setOnClickListener {
            startActivity(Intent(this, DeleteConfirmationActivity::class.java))
        }
        binding.Addshop.setOnClickListener {
            startActivity(Intent(this, AddNewShop::class.java))
        }
        binding.logout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
        binding.viewshop.setOnClickListener {
            startActivity(Intent(this, ViewShopActivity::class.java))
        }
        binding.viewcustomer.setOnClickListener {
            startActivity(Intent(this, CustomerAccounts::class.java))
        }
    }

    private fun disableSearchViewInput(searchView: androidx.appcompat.widget.SearchView) {
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchView.clearFocus()
                startActivity(Intent(this, ShopSearchActivity::class.java))

            }
        }
    }
    private fun showLogoutConfirmationDialog() {
        // Create a dialog instance
        val dialog = Dialog(this)
        // Inflate the custom layout
        val dialogView: View = LayoutInflater.from(this).inflate(R.layout.dialog_logout_confirmation, null)
        dialog.setContentView(dialogView)

        // Get references to the Yes and No buttons in the custom layout
        val btnDialogYes = dialogView.findViewById<Button>(R.id.btnDialogYes)
        val btnDialogNo = dialogView.findViewById<Button>(R.id.btnDialogNo)

        // Handle Yes button click
        btnDialogYes.setOnClickListener {
            logout()  // Call the logout function
            dialog.dismiss() // Close the dialog
        }

        // Handle No button click
        btnDialogNo.setOnClickListener {
            dialog.dismiss()  // Just close the dialog
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // Show the dialog

    }
    private fun logout() {
        googleSignInClient.signOut().addOnCompleteListener(this, OnCompleteListener {
            // After signing out, move to login activity
            val intent = Intent(this@MainAddNewShopActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        })
    }
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()  // Close the app completely
    }

}
