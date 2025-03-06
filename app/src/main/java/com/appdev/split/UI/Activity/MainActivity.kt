package com.appdev.split.UI.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.appdev.split.R
import com.appdev.split.Utils.ThemeUtils
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (ThemeUtils.isDarkMode(this)) {
            ThemeUtils.setStatusBarDark(this, R.color.darkBackground)
        } else {
            ThemeUtils.setStatusBarLight(this, R.color.screenBack)
        }

        val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

        if (!Utils.isOnboardingPassed(this)) {
            startActivity(Intent(this, OnBoardingActivity::class.java))
            finish()
        } else if (firebaseAuth.currentUser != null) {
            val intent = Intent(this, EntryActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        binding.btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
    }
}