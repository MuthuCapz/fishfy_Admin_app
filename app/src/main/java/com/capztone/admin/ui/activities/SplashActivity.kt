package com.capztone.admin.ui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivitySplashBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var databaseReference: DatabaseReference // Declare the database reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val isFirstTimeLogin = sharedPreferences.getBoolean("isFirstTimeLogin", true)

        // Animation setup (remains unchanged)
        val translateAnimation = TranslateAnimation(0f, 500f, 500f, 0f)
        translateAnimation.duration = 1000

        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_animation)

        val animationSet = AnimationSet(true)
        animationSet.addAnimation(translateAnimation)
        animationSet.addAnimation(rotateAnimation)

        binding.iconTxt.startAnimation(zoomInAnimation)


        val animationView: LottieAnimationView = binding.lottieAnimationView
        animationView.setAnimation("admin.json")
        animationView.playAnimation()

        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)

        // Retrieve the "General Admin" email from Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("General Admin").child("Email")
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val generalAdminEmail = dataSnapshot.getValue(String::class.java)

                Handler().postDelayed({
                    if (account != null) {
                        val currentEmail = account.email

                        if (currentEmail == generalAdminEmail) {
                            if (isFirstTimeLogin) {
                                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                            } else {
                                startActivity(Intent(this@SplashActivity, MainAddNewShopActivity::class.java))
                            }
                        } else {
                            // Check if the email is in the "Admins" path
                            val adminsReference = FirebaseDatabase.getInstance().getReference("Admins")
                            adminsReference.orderByChild("email").equalTo(currentEmail)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        if (dataSnapshot.exists()) {
                                            // Email found in Admins, navigate to SubAdminMainActivity
                                            startActivity(Intent(this@SplashActivity, SubAdminMainActivity::class.java))
                                        } else {
                                            // Email not found, navigate to LoginActivity
                                            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                                        }
                                        // Save the last logged-in email and update isFirstTimeLogin flag
                                        sharedPreferences.edit().putString("lastLoggedInEmail", currentEmail).apply()
                                        sharedPreferences.edit().putBoolean("isFirstTimeLogin", false).apply()
                                        finish()
                                    }

                                    override fun onCancelled(databaseError: DatabaseError) {
                                        // Handle potential errors
                                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                                        finish()
                                    }
                                })
                        }
                    } else {
                        // No account signed in, navigate to LoginActivity
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        finish()
                    }
                }, SPLASH_SCREEN_DURATION)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle potential errors
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                finish()
            }
        })
    }

    companion object {
        private const val SPLASH_SCREEN_DURATION = 3500L
    }
}