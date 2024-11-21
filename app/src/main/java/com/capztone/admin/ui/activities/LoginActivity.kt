package com.capztone.admin.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivityLoginBinding
import com.capztone.admin.utils.FirebaseAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var binding: ActivityLoginBinding
    private var adminEmail: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

auth = FirebaseAuthUtil.auth
        configureGoogleSignIn()

        // Retrieve admin email from Firebase
        FirebaseDatabase.getInstance().getReference("General Admin/Email")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    adminEmail = snapshot.getValue(String::class.java) // Retrieve the email
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Failed to read admin email.", error.toException())
                }
            })
        binding.googleLoginbutton.setOnClickListener {
            signIn()
        }

    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_Id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    val email = user?.email ?: ""
                    val userId = user?.uid ?: ""
                    val username = user?.displayName ?: ""

                    // Save username and email to SharedPreferences
                    val sharedPreferences = getSharedPreferences("user_info", MODE_PRIVATE)
                    with(sharedPreferences.edit()) {
                        putString("USERNAME", username)
                        putString("EMAIL", email)
                        apply() // Save changes
                    }

                    // Check if the user is logging in for the first time
                    val isFirstTime = sharedPreferences.getBoolean("isFirstTime_$email", true)

                    // Define intent variable
                    var intent: Intent? = null

                    // Check if the email is itworkjob01@gmail.com
                    if (email == adminEmail) {
                        // Handle the specific case for this email
                        intent = if (isFirstTime) {
                            sharedPreferences.edit().putBoolean("isFirstTime_$email", false).apply()
                            Intent(this, PasswordActivity::class.java)
                        } else {
                            Intent(this, MainAddNewShopActivity::class.java)
                        }
                    } else {
                        // Check if the email exists in ShopNames
                        val shopNamesRef = FirebaseDatabase.getInstance().getReference("ShopNames")
                        shopNamesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                var isEmailValid = false

                                // Iterate through each shop ID to check the shopMailId
                                for (shopSnapshot in snapshot.children) {
                                    val shopMailId = shopSnapshot.child("shopMailId").getValue(String::class.java)
                                    if (shopMailId == email) {
                                        isEmailValid = true
                                        break // Exit the loop if a matching email is found
                                    }
                                }

                                // Check if the user ID is present in Admins node
                                if (isEmailValid) {
                                    val adminsRef = FirebaseDatabase.getInstance().getReference("Admins")
                                    adminsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(adminSnapshot: DataSnapshot) {
                                            val isUserAdmin = adminSnapshot.hasChild(userId)

                                            // Determine the intent based on Admins check
                                            intent = if (isUserAdmin) {
                                                Intent(this@LoginActivity, SubAdminMainActivity::class.java)
                                            } else {
                                                if (isFirstTime) {
                                                    sharedPreferences.edit().putBoolean("isFirstTime_$email", false).apply()
                                                }
                                                Intent(this@LoginActivity, SubAdminsPassword::class.java)
                                            }

                                            // Start the activity if intent is not null
                                            intent?.let {
                                                it.putExtra("USERNAME", username)
                                                it.putExtra("EMAIL", email)
                                                startActivity(it)
                                                finish()
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Log.w(TAG, "Failed to read Admins.", error.toException())
                                        }
                                    })
                                } else {
                                    // Show a toast message if the email is not valid
                                    Toast.makeText(this@LoginActivity, "Choose a valid shop account to login.", Toast.LENGTH_SHORT).show()
                                    logout()

                                    // Call the method to prompt the Google login options again
                                    startGoogleSignIn()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Log.w(TAG, "Failed to read ShopNames.", error.toException())
                            }
                        })
                    }

                    // If intent was created for itworkjob01, start it here
                    intent?.let {
                        it.putExtra("USERNAME", username)
                        it.putExtra("EMAIL", email)
                        startActivity(it)
                        finish()
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }



    // Method to start Google sign-in process
    private fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }


    private fun logout() {
        googleSignInClient.signOut()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        // This method closes the app when the back button is pressed in MainActivity
        finishAffinity()
    }

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }
}