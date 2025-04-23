package com.appdev.split.UI.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.appdev.split.R
import com.appdev.split.Utils.ThemeUtils
import com.appdev.split.databinding.ActivityForgotPasswordBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ForgotPassword : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set status bar color based on theme
        if (ThemeUtils.isDarkMode(this)) {
            ThemeUtils.setStatusBarDark(this, R.color.darkBackground)
        } else {
            ThemeUtils.setStatusBarLight(this, R.color.screenBack)
        }

        // Pre-fill email if provided
        if (intent.getStringExtra("email")?.toString() != "null") {
            binding.etEmail.editText?.setText(intent.getStringExtra("email"))
        }

        // Set up button listeners
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnResetPassword.setOnClickListener {
            resetPassword()
        }
    }

    private fun resetPassword() {
        val email = binding.etEmail.editText?.text.toString().trim()

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading()

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                hideLoading()
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset link sent to your email",
                        Toast.LENGTH_LONG
                    ).show()

                    // Return to login screen with the email
                    val intent = Intent(this, Login::class.java)
                    intent.putExtra("email", email)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        task.exception?.message ?: "Failed to send reset link",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun showLoading() {
        binding.loadingLayout.apply {
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        }
    }

    private fun hideLoading() {
        binding.loadingLayout.apply {
            animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    visibility = View.GONE
                }
                .start()
        }
    }
}