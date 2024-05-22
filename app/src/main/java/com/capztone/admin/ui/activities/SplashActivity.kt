package com.capztone.admin.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
import com.capztone.admin.MainActivity
import com.capztone.admin.R
import com.capztone.admin.databinding.ActivitySplashBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val translateAnimation = TranslateAnimation(0f, 500f, 500f, 0f)
        translateAnimation.duration = 1000 // Set duration as needed

        // Rotate and zoom animations
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
        val zoomInAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in_animation)

        // Animate simultaneously
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(translateAnimation)
        animationSet.addAnimation(rotateAnimation)

        binding.iconTxt.startAnimation(zoomInAnimation)

        // Start animation
        val animationView: LottieAnimationView = binding.lottieAnimationView
        animationView.setAnimation("admin.json")
        animationView.playAnimation()

        // Check if user is already signed in with Google
        val account = GoogleSignIn.getLastSignedInAccount(this)

        // Navigate to appropriate activity after a delay
        Handler().postDelayed({
            if (account != null) {
                // User is already signed in, navigate to MainActivity
                startActivity(Intent(this, com.capztone.admin.MainActivity::class.java))
            } else {
                // User is not signed in, navigate to LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
            }
            finish()
        }, SPLASH_SCREEN_DURATION)
    }

    companion object {
        private const val SPLASH_SCREEN_DURATION = 3500L // 3 seconds
    }
}