package com.appdev.split.UI.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import com.appdev.split.Model.Data.UserEntity
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.databinding.ActivityLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Login : AppCompatActivity() {
    lateinit var binding: ActivityLoginBinding

    val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.getStringExtra("email").toString() != "null")
            binding.etEmail.editText?.setText(intent.getStringExtra("email").toString())

        binding.btnBackLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnDoneLogin.setOnClickListener {
            val email = binding.etEmail.editText?.text.toString()
            val password = binding.etPassword.editText?.text.toString()
            if (email.trim().isNotEmpty()
                && password.trim().isNotEmpty()
            ) {
                val userEntity = UserEntity("", email, password)
                mainViewModel.startLogin(userEntity) { message, success ->
                    if (success) {
                        val intent2 = Intent(this, EntryActivity::class.java)
                        startActivity(intent2)
                        this.finish()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }
}